package org.janusgraph;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.example.GraphOfTheGodsFactory;
import org.janusgraph.graphdb.transaction.StandardJanusGraphTx;

/**
 * Hello world!
 *
 */
public class App 
{
    private static final String tableName = "graph";
    public static void main( String[] args )
    {

        System.out.println( "Hello World!" );

        JanusGraph graph = JanusGraphFactory
            .build()
            .set("storage.hbase.table",tableName)
            .set("storage.backend", "hbase")
            .set("storage.hostname", "10.33.50.6, 10.33.50.7, 10.33.50.8")
            .open();
        GraphOfTheGodsFactory.loadWithoutMixedIndex(graph, true);

        StandardJanusGraphTx tx = (StandardJanusGraphTx) graph.newTransaction();

        GraphTraversalSource g = tx.traversal();

        GraphTraversal<Vertex, Vertex> hasSaturn = g.V().has("name", "saturn");
        Vertex saturmn = hasSaturn.next();
        GraphTraversal<Vertex, Object> values = g.V(saturmn).in("father").in("father").values("name");
        System.out.println(values.next());
        //JanusGraph open = JanusGraphFactory.open("inmemory");

    }
}
