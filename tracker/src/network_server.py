"""
Network Server
TCP server that sends FT8 decodes to Android Auto app
"""

import socket
import threading
import logging
import queue
from typing import List, Dict, Any
from datetime import datetime

logger = logging.getLogger(__name__)


class NetworkServer:
    """TCP server for sending decodes to Android Auto app"""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.host = config.get('server_bind', '0.0.0.0')
        self.port = config.get('server_port', 8080)
        self.running = False
        self.server_socket = None
        self.clients = []
        self.decode_queue = queue.Queue()
        self.server_thread = None
        self.broadcast_thread = None
        
    def start(self):
        """Start the TCP server"""
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
            
            logger.info(f"Network server started on {self.host}:{self.port}")
            return True
            
        except Exception as e:
            logger.error(f"Failed to start network server: {e}")
            return False
            
    def stop(self):
        """Stop the server"""
        self.running = False
        
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
