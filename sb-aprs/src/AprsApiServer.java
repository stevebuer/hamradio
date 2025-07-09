import java.io.*;
import java.net.*;
import com.sun.net.httpserver.*;

/**
 * run HTTP server to provide API for Android app
 */

public class AprsApiServer implements Runnable {

	/* http server instance */

	private HttpServer srv;

	/* default HTTP port */

	private int port = 8000;

	/* Shared APRS Context */

	AprsContext context;

	/* constructors */

 	AprsApiServer() { }

	AprsApiServer(AprsContext c) {

		context = c;
	}

	/* start web server */

	public void run() {

		try {

			srv = HttpServer.create(new InetSocketAddress(this.port), 0);

		} catch (Exception e) {

			SBLog.log("Error starting server: " + e.getMessage());
			return;
		}

		srv.createContext("/mheard", new MheardHandler(context));

		srv.createContext("/graph", new GraphHandler(context));
		
		srv.createContext("/status", new StatusHandler(context));

		srv.start();

		SBLog.log("API server started...");     
	}
	
	/** make status handler page */
	
	static class StatusHandler implements HttpHandler {

		AprsContext ctx;

                public StatusHandler(AprsContext ctx) { 
		
			this.ctx = ctx; 
		}

		/* process request */
	
                public void handle(HttpExchange x) throws IOException {

			// STATS: maxdistance, total packets, total stations, etc. all the things

			String r =  "NODES: " + ctx.mheardMap.size() + "\n";

			r += "MAX_DISTANCE: " + String.format("%.4f", ctx.distanceMap.values().stream().max(Double::compare).get()) + "\n";

                        x.sendResponseHeaders(200, r.getBytes().length);

                        OutputStream o = x.getResponseBody();

                        o.write(r.getBytes());

                        o.close();
                }
	}

	/** make graph handler page */

	static class GraphHandler implements HttpHandler {

		AprsContext ctx;

                public GraphHandler(AprsContext ctx) { 
		
			this.ctx = ctx; 
		}

		/* process request */
	
                public void handle(HttpExchange x) throws IOException {

			String r = AprsGraph.outputGraph();

                        x.sendResponseHeaders(200, r.getBytes().length);

                        OutputStream o = x.getResponseBody();

                        o.write(r.getBytes());

                        o.close();
                }
	}


	/** class for handling mheard requests */

	static class MheardHandler implements HttpHandler {

		AprsContext ctx;

                public MheardHandler(AprsContext ctx) { 
		
			this.ctx = ctx; 
		}

		/* process request */
	
                public void handle(HttpExchange x) throws IOException {

			/* try some json? : or maybe just html/text... */

			// x.getResponseHeaders().set("Content-Type", "application/json");

			// just use CSV for now...
                
                        // String r = "{\n\"mycall\": " + "\"" + ctx.myCall() + "\",\n\"mheard\": ";

			String r = ctx.mheard();

                        x.sendResponseHeaders(200, r.getBytes().length);

                        OutputStream o = x.getResponseBody();

                        o.write(r.getBytes());

                        o.close();
                }
    	}
}
