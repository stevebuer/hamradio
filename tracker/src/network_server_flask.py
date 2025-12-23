"""
Network Server - Flask-based REST API with Server-Sent Events (SSE)
Provides:
- FT8 decode stream via HTTP Server-Sent Events (SSE)
- REST endpoints for GPS updates (/gps)
- REST endpoints for band changes (/band)
- Health check endpoint (/health)
All on a single port and elegant REST architecture
"""

import logging
import json
import threading
from typing import Dict, Any, Optional, Callable, List
from datetime import datetime
from flask import Flask, request, Response, jsonify
import queue

logger = logging.getLogger(__name__)


class FlaskNetworkServer:
    """Flask-based network server for FT8 tracker with SSE support"""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.host = config.get('server_bind', '0.0.0.0')
        self.port = int(config.get('server_port', 8080))
        self.running = False
        
        # Create Flask app
        self.app = Flask('FT8Tracker')
        self._setup_routes()
        
        # Decode streaming via SSE
        self.decode_queue = queue.Queue()
        self.sse_clients: List[queue.Queue] = []
        self.sse_lock = threading.Lock()
        
        # State
        self.current_band: Optional[str] = None
        self.last_gps_update: Optional[Dict[str, Any]] = None
        
        # Callbacks
        self.gps_callback: Optional[Callable] = None
        self.band_callback: Optional[Callable] = None
        
        # Flask thread
        self.flask_thread = None
        
    def _setup_routes(self):
        """Setup Flask routes"""
        
        @self.app.route('/decodes', methods=['GET'])
        def get_decodes_stream():
            """Server-Sent Events stream for FT8 decodes"""
            logger.debug("New SSE client connected")
            
            # Create a queue for this client
            client_queue = queue.Queue()
            
            with self.sse_lock:
                self.sse_clients.append(client_queue)
                logger.debug(f"Added SSE client, total clients: {len(self.sse_clients)}")
            
            def generate():
                """Generate SSE messages for this client"""
                try:
                    while self.running:
                        try:
                            # Get decode from client's queue (timeout to check if connection alive)
                            decode_line = client_queue.get(timeout=5.0)
                            # Format as SSE
                            yield f"data: {json.dumps({'decode': decode_line})}\n\n"
                        except queue.Empty:
                            # Send keepalive
                            yield f": keepalive\n\n"
                        except Exception as e:
                            logger.debug(f"SSE client error: {e}")
                            break
                finally:
                    # Remove client queue when generator is done
                    logger.debug("SSE client disconnecting")
                    with self.sse_lock:
                        if client_queue in self.sse_clients:
                            self.sse_clients.remove(client_queue)
                            logger.debug(f"Removed SSE client, total clients: {len(self.sse_clients)}")
            
            return Response(generate(), mimetype='text/event-stream', 
                           headers={
                               'Cache-Control': 'no-cache',
                               'X-Accel-Buffering': 'no',
                               'Access-Control-Allow-Origin': '*'
                           })
        
        @self.app.route('/gps', methods=['POST'])
        def handle_gps():
            """Handle GPS position update"""
            try:
                gps_data = request.get_json()
                
                if not gps_data:
                    return jsonify({'error': 'Empty JSON'}), 400
                
                logger.debug(f"Received GPS update: {gps_data}")
                
                # Validate required fields
                if 'latitude' not in gps_data or 'longitude' not in gps_data:
                    return jsonify({'error': 'Missing latitude or longitude'}), 400
                
                # Add timestamp if not present
                if 'timestamp' not in gps_data:
                    gps_data['timestamp'] = int(datetime.now().timestamp())
                
                # Store GPS update
                self.last_gps_update = gps_data
                
                # Call callback if registered
                if self.gps_callback:
                    logger.debug("Calling GPS callback")
                    try:
                        self.gps_callback(gps_data)
                    except Exception as e:
                        logger.error(f"GPS callback error: {e}")
                
                logger.info(f"GPS position received: {gps_data.get('latitude')}, {gps_data.get('longitude')}")
                
                return jsonify({
                    'status': 'ok',
                    'message': 'GPS position received'
                }), 200
                
            except json.JSONDecodeError:
                return jsonify({'error': 'Invalid JSON'}), 400
            except Exception as e:
                logger.error(f"Error processing GPS update: {e}")
                return jsonify({'error': 'Internal server error'}), 500
        
        @self.app.route('/band', methods=['POST'])
        def handle_band():
            """Handle band change notification"""
            try:
                band_data = request.get_json()
                
                if not band_data:
                    return jsonify({'error': 'Empty JSON'}), 400
                
                logger.debug(f"Received band change request: {band_data}")
                
                # Validate required fields
                if 'band' not in band_data:
                    return jsonify({'error': 'Missing band field'}), 400
                
                band = band_data['band']
                
                # Validate band format
                valid_bands = ['80m', '60m', '40m', '30m', '20m', '17m', '15m', '12m', '10m', '6m']
                if band not in valid_bands:
                    return jsonify({
                        'error': f'Invalid band. Must be one of: {", ".join(valid_bands)}'
                    }), 400
                
                logger.debug(f"Band validated: {band}")
                
                # Store current band
                self.current_band = band
                logger.debug(f"Band stored in server: {band}")
                
                # Call callback if registered
                if self.band_callback:
                    logger.debug(f"Calling band callback for: {band}")
                    try:
                        self.band_callback(band)
                    except Exception as e:
                        logger.error(f"Band callback error: {e}")
                
                logger.info(f"Band changed to: {band}")
                
                return jsonify({
                    'status': 'ok',
                    'message': f'Band set to {band}'
                }), 200
                
            except json.JSONDecodeError:
                return jsonify({'error': 'Invalid JSON'}), 400
            except Exception as e:
                logger.error(f"Error processing band update: {e}")
                return jsonify({'error': 'Internal server error'}), 500
        
        @self.app.route('/health', methods=['GET'])
        def health_check():
            """Health check endpoint"""
            return jsonify({
                'status': 'ok',
                'current_band': self.current_band,
                'last_gps': self.last_gps_update,
                'sse_clients': len(self.sse_clients)
            }), 200
        
        @self.app.errorhandler(404)
        def not_found(error):
            """Handle 404 errors"""
            return jsonify({'error': 'Not found'}), 404
        
        @self.app.errorhandler(500)
        def server_error(error):
            """Handle 500 errors"""
            logger.error(f"Server error: {error}")
            return jsonify({'error': 'Internal server error'}), 500
    
    def start(self):
        """Start the Flask server"""
        try:
            self.running = True
            
            logger.info(f"Starting Flask network server on {self.host}:{self.port}")
            
            # Start Flask in a separate thread
            self.flask_thread = threading.Thread(
                target=self.app.run,
                kwargs={
                    'host': self.host,
                    'port': self.port,
                    'debug': False,
                    'use_reloader': False,
                    'threaded': True
                }
            )
            self.flask_thread.daemon = True
            self.flask_thread.start()
            
            logger.info(f"Flask server started on {self.host}:{self.port}")
            logger.info(f"  REST API endpoints:")
            logger.info(f"    GET  /decodes  - Server-Sent Events stream for FT8 decodes")
            logger.info(f"    POST /gps      - GPS position update")
            logger.info(f"    POST /band     - Band change notification")
            logger.info(f"    GET  /health   - Health check")
            
            return True
            
        except Exception as e:
            logger.error(f"Failed to start network server: {e}")
            return False
    
    def stop(self):
        """Stop the Flask server"""
        self.running = False
        logger.info("Network server stopping...")
        # Flask shutdown is tricky, but daemon thread will die with main process
    
    def send_decode(self, decode_line: str):
        """Queue a decode to be sent to all SSE clients"""
        with self.sse_lock:
            for client_queue in self.sse_clients[:]:
                try:
                    client_queue.put_nowait(decode_line)
                except queue.Full:
                    logger.warning("Client queue full, dropping decode")
    
    def has_clients(self) -> bool:
        """Check if there are any connected SSE clients"""
        with self.sse_lock:
            return len(self.sse_clients) > 0
    
    def get_client_count(self) -> int:
        """Get number of connected SSE clients"""
        with self.sse_lock:
            return len(self.sse_clients)
    
    def set_gps_callback(self, callback: Callable):
        """Set callback for GPS updates"""
        self.gps_callback = callback
    
    def set_band_callback(self, callback: Callable):
        """Set callback for band changes"""
        self.band_callback = callback
    
    def get_current_band(self) -> Optional[str]:
        """Get the current operating band"""
        return self.current_band
    
    def get_last_gps_update(self) -> Optional[Dict[str, Any]]:
        """Get the last GPS position received"""
        return self.last_gps_update


if __name__ == '__main__':
    # Test Flask server
    import time
    
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    config = {
        'server_bind': '0.0.0.0',
        'server_port': 8080
    }
    
    server = FlaskNetworkServer(config)
    
    if server.start():
        try:
            print(f"Server running on port {config['server_port']}")
            print("Connect to /decodes endpoint with SSE client")
            print("Press Ctrl+C to stop")
            
            # Send test decodes
            counter = 0
            while True:
                time.sleep(5)
                counter += 1
                
                # Generate test decode
                now = datetime.now()
                time_str = now.strftime("%H%M%S")
                test_decode = f"{time_str} -12  0.3 1234 ~ CQ TEST{counter} FN42"
                
                server.send_decode(test_decode)
                print(f"Sent: {test_decode} (clients: {server.get_client_count()})")
                
        except KeyboardInterrupt:
            print("\nStopping server...")
            server.stop()
