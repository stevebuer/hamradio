"""
Network Server
Combined TCP+HTTP server that:
- Sends FT8 decodes to Android Auto app via TCP stream
- Receives GPS position updates via HTTP POST /gps endpoint
- Receives band changes via HTTP POST /band endpoint
"""

import socket
import threading
import logging
import queue
import json
from http.server import HTTPServer, BaseHTTPRequestHandler
from typing import List, Dict, Any, Optional, Callable
from datetime import datetime

logger = logging.getLogger(__name__)


class NetworkServer:
    """Combined TCP+HTTP server for decodes and GPS updates"""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.host = config.get('server_bind', '0.0.0.0')
        self.port = int(config.get('server_port', 8080))
        # HTTP server uses same port by default, but can be overridden
        self.http_port = int(config.get('server_http_port', self.port))
        self.running = False
        self.server_socket = None
        self.clients = []
        self.decode_queue = queue.Queue()
        self.server_thread = None
        self.broadcast_thread = None
        self.http_server = None
        self.http_thread = None
        self.gps_callback: Optional[Callable] = None
        self.last_gps_update: Optional[Dict[str, Any]] = None
        self.band_callback: Optional[Callable] = None
        self.current_band: Optional[str] = None
        
    def start(self):
        """Start the combined TCP+HTTP server"""
        try:
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.server_socket.bind((self.host, self.port))
            self.server_socket.listen(5)
            self.server_socket.settimeout(1.0)
            
            self.running = True
            
            # Start accept thread
            self.server_thread = threading.Thread(target=self._accept_clients)
            self.server_thread.daemon = True
            self.server_thread.start()
            
            # Start broadcast thread
            self.broadcast_thread = threading.Thread(target=self._broadcast_loop)
            self.broadcast_thread.daemon = True
            self.broadcast_thread.start()
            
            # Start HTTP server on same port for GPS/band updates
            self._start_http_server()
            
            logger.info(f"Network server started on {self.host}:{self.port}")
            logger.info(f"  TCP stream for FT8 decodes on {self.host}:{self.port}")
            if self.http_port != self.port:
                logger.info(f"  HTTP endpoints (/gps, /band) on {self.host}:{self.http_port}")
            else:
                logger.info(f"  HTTP endpoints (/gps, /band) on {self.host}:{self.http_port}")
            return True
            
        except Exception as e:
            logger.error(f"Failed to start network server: {e}")
            return False
            
    def stop(self):
        """Stop the server"""
        self.running = False
        
        # Stop HTTP server
        if self.http_server:
            self.http_server.shutdown()
            self.http_server = None
        
        # Close all client connections
        for client in self.clients[:]:
            try:
                client.close()
            except:
                pass
        self.clients.clear()
        
        # Close server socket
        if self.server_socket:
            try:
                self.server_socket.close()
            except:
                pass
                
        logger.info("Network server stopped")
        
    def _accept_clients(self):
        """Accept incoming client connections"""
        while self.running:
            try:
                client_socket, address = self.server_socket.accept()
                logger.info(f"Client connected: {address}")
                self.clients.append(client_socket)
                
                # Send welcome message
                welcome = f"# FT8 Tracker - Connected at {datetime.now()}\n"
                self._send_to_client(client_socket, welcome)
                
            except socket.timeout:
                continue
            except Exception as e:
                if self.running:
                    logger.error(f"Error accepting client: {e}")
                    
    def _broadcast_loop(self):
        """Broadcast decodes to all connected clients"""
        while self.running:
            try:
                # Get decode from queue (with timeout)
                try:
                    decode_line = self.decode_queue.get(timeout=1.0)
                except queue.Empty:
                    continue
                    
                # Send to all clients
                for client in self.clients[:]:
                    if not self._send_to_client(client, decode_line + "\n"):
                        # Remove disconnected client
                        self.clients.remove(client)
                        
            except Exception as e:
                logger.error(f"Error in broadcast loop: {e}")
                
    def _send_to_client(self, client_socket: socket.socket, message: str) -> bool:
        """Send message to a client, return False if failed"""
        try:
            client_socket.sendall(message.encode('utf-8'))
            return True
        except Exception as e:
            logger.warning(f"Failed to send to client: {e}")
            try:
                client_socket.close()
            except:
                pass
            return False
            
    def send_decode(self, decode_line: str):
        """Queue a decode to be sent to clients"""
        self.decode_queue.put(decode_line)
        
    def get_client_count(self) -> int:
        """Get number of connected clients"""
        return len(self.clients)
    
    def set_gps_callback(self, callback: Callable):
        """Set callback for GPS updates from Android app"""
        self.gps_callback = callback
    
    def set_band_callback(self, callback: Callable):
        """Set callback for band changes from Android app"""
        self.band_callback = callback
    
    def get_current_band(self) -> Optional[str]:
        """Get the current operating band"""
        return self.current_band
    
    def get_last_gps_update(self) -> Optional[Dict[str, Any]]:
        """Get the last GPS position received from Android app"""
        return self.last_gps_update
    
    def _start_http_server(self):
        """Start HTTP server on same port as TCP for GPS/band updates"""
        parent = self
        
        class GPSRequestHandler(BaseHTTPRequestHandler):
            """Handle HTTP requests for GPS updates and band changes"""
            
            def log_message(self, format, *args):
                """Override to use our logger"""
                logger.debug(format % args)
            
            def do_POST(self):
                """Handle POST request for GPS update or band change"""
                if self.path == '/gps':
                    try:
                        content_length = int(self.headers.get('Content-Length', 0))
                        if content_length > 10000:  # 10KB max
                            self.send_error(413, "Payload too large")
                            return
                        
                        body = self.rfile.read(content_length)
                        gps_data = json.loads(body.decode('utf-8'))
                        
                        # Validate required fields
                        if 'latitude' not in gps_data or 'longitude' not in gps_data:
                            self.send_error(400, "Missing latitude or longitude")
                            return
                        
                        # Add timestamp if not present
                        if 'timestamp' not in gps_data:
                            gps_data['timestamp'] = int(datetime.now().timestamp())
                        
                        # Store GPS update
                        parent.last_gps_update = gps_data
                        
                        # Call callback if registered
                        if parent.gps_callback:
                            try:
                                parent.gps_callback(gps_data)
                            except Exception as e:
                                logger.error(f"GPS callback error: {e}")
                        
                        # Send success response
                        self.send_response(200)
                        self.send_header('Content-type', 'application/json')
                        self.end_headers()
                        response = {'status': 'ok', 'message': 'GPS position received'}
                        self.wfile.write(json.dumps(response).encode('utf-8'))
                        
                        logger.info(f"Received GPS: {gps_data.get('latitude')}, {gps_data.get('longitude')}")
                        
                    except json.JSONDecodeError:
                        self.send_error(400, "Invalid JSON")
                    except Exception as e:
                        logger.error(f"Error processing GPS update: {e}")
                        self.send_error(500, "Internal server error")
                
                elif self.path == '/band':
                    try:
                        content_length = int(self.headers.get('Content-Length', 0))
                        if content_length > 1000:  # 1KB max
                            self.send_error(413, "Payload too large")
                            return
                        
                        body = self.rfile.read(content_length)
                        band_data = json.loads(body.decode('utf-8'))
                        
                        # Validate required fields
                        if 'band' not in band_data:
                            self.send_error(400, "Missing band field")
                            return
                        
                        band = band_data['band']
                        
                        logger.debug(f"Received band change request: {band}")
                        
                        # Validate band format (e.g., "40m", "20m", "15m")
                        valid_bands = ['80m', '60m', '40m', '30m', '20m', '17m', '15m', '12m', '10m', '6m']
                        if band not in valid_bands:
                            self.send_error(400, f"Invalid band. Must be one of: {', '.join(valid_bands)}")
                            return
                        
                        # Store current band
                        parent.current_band = band
                        logger.debug(f"Band stored in network server: {band}")
                        
                        # Call callback if registered
                        if parent.band_callback:
                            logger.debug(f"Calling band callback for: {band}")
                            try:
                                parent.band_callback(band)
                            except Exception as e:
                                logger.error(f"Band callback error: {e}")
                        
                        # Send success response
                        self.send_response(200)
                        self.send_header('Content-type', 'application/json')
                        self.end_headers()
                        response = {'status': 'ok', 'message': f'Band set to {band}'}
                        self.wfile.write(json.dumps(response).encode('utf-8'))
                        
                        logger.info(f"Band changed to: {band}")
                        
                    except json.JSONDecodeError:
                        self.send_error(400, "Invalid JSON")
                    except Exception as e:
                        logger.error(f"Error processing band update: {e}")
                        self.send_error(500, "Internal server error")
                
                else:
                    self.send_error(404, "Not found")
            
            def do_GET(self):
                """Handle GET request - health check"""
                if self.path == '/health':
                    self.send_response(200)
                    self.send_header('Content-type', 'application/json')
                    self.end_headers()
                    response = {
                        'status': 'ok',
                        'last_gps': parent.last_gps_update,
                        'current_band': parent.current_band
                    }
                    self.wfile.write(json.dumps(response).encode('utf-8'))
                else:
                    self.send_error(404, "Not found")
        
        try:
            # Create HTTP server with REUSEADDR to allow binding to same port
            logger.debug(f"Attempting to start HTTP server on {self.host}:{self.http_port}")
            self.http_server = HTTPServer((self.host, self.http_port), GPSRequestHandler)
            # Allow reuse of the address
            self.http_server.allow_reuse_address = True
            logger.debug(f"Starting HTTP server on {self.host}:{self.http_port}")
            self.http_thread = threading.Thread(target=self.http_server.serve_forever)
            self.http_thread.daemon = True
            self.http_thread.start()
            logger.info(f"HTTP server started successfully on {self.host}:{self.http_port}")
        except OSError as e:
            if "Address already in use" in str(e):
                logger.error(f"Failed to start HTTP server on {self.http_port}: Port already in use.")
                logger.warning("HTTP endpoints (/gps, /band) will not be available")
            else:
                logger.error(f"Failed to start HTTP server: {e}")
        except Exception as e:
            logger.error(f"Failed to start HTTP server: {e}")


if __name__ == '__main__':
    # Test network server
    import time
    import sys
    
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    config = {
        'server_bind': '0.0.0.0',
        'server_port': 8080
    }
    
    server = NetworkServer(config)
    
    if server.start():
        try:
            print(f"Server running on port {config['server_port']}")
            print("Connect with: nc localhost 8080")
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
    else:
        print("Failed to start server")
        sys.exit(1)
