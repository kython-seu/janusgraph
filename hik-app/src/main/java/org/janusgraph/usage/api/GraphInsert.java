package org.janusgraph.usage.api;

import org.apache.tinkerpop.gremlin.structure.T;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.JanusGraphTransaction;

/**
 * Created by IBM on 2018/1/26.
 */
public class GraphInsert {

    public static JanusGraph janusGraph;
    static {
        //janusGraph = JanusGraphFactory.build().set("storage.backend", "inmemory").open();
        janusGraph = JanusGraphFactory.open("inmemory");
    }
    public static void main(String[] args) {
        insertOne();
    }

    public static void insertOne(){
        JanusGraphTransaction tx = janusGraph.newTransaction();
        tx.addVertex(T.label, "person", "userName", "lucy", "age", 29);

        tx.commit();
        janusGraph.close();
    }
}
