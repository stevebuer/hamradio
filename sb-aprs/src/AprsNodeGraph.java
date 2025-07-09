import java.util.*;

public class AprsNodeGraph {

	private TreeSet<String> edges, vertices;

	AprsNodeGraph() { 

		edges = new TreeSet<String>();	
		vertices = new TreeSet<String>();	
	}

	public void addVertex(String v) {

		edges.add(v);
	}

	public void addEdge(String e) {

		vertices.add(e);
	}

}
