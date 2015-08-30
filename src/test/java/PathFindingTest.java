import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Paths;
import org.neo4j.test.TestGraphDatabaseFactory;

import java.util.stream.IntStream;

public class PathFindingTest {
    private static GraphDatabaseService graphDb;
    private Transaction tx;
    private final Label stationLabel = DynamicLabel.label("Station");
    private final Label trainLabel = DynamicLabel.label("Train");

    private enum ConnectionTypes implements RelationshipType {
        ARRIVAL,
        DEPARTURE
    }

    private static final String ARRIVAL = "arrival";
    private static final String DEPARTURE = "departure";

    @BeforeClass
    public static void setUpDb() {
        graphDb = new TestGraphDatabaseFactory().newImpermanentDatabase();
    }

    @Before
    public void setUp() {
        tx = graphDb.beginTx();
    }

    @After
    public void doAfter() {
        tx.success();
        tx.close();
    }

    @AfterClass
    public static void tearDown() {
        graphDb.shutdown();
    }

    @Test
    public void shortestPaths() {
        final Node[] stationNodes = IntStream.rangeClosed(0, 4)
                .mapToObj(x -> createNode(stationLabel, x)).toArray(Node[]::new);
        final Node[] trainNodes = IntStream.rangeClosed(0, 2)
                .mapToObj(x -> createNode(trainLabel, x)).toArray(Node[]::new);

        createDeparture(stationNodes[0], trainNodes[0], 1000);
        createStop(stationNodes[1], trainNodes[0], 1100, 1200);
        createArrival(stationNodes[2], trainNodes[0], 1300);

        createDeparture(stationNodes[0], trainNodes[1], 1030);
        createArrival(stationNodes[2], trainNodes[1], 1250);

        createDeparture(stationNodes[2], trainNodes[2], 1330);
        createArrival(stationNodes[3], trainNodes[2], 1400);

        printGraph();

        printShortestPathsDummy(stationNodes[0], stationNodes[3]);


    }

    private static void printShortestPathsDummy(final Node startStation, final Node endStation) {
        final PathFinder<Path> finder = GraphAlgoFactory.shortestPath(PathExpanders.forDirection(Direction.OUTGOING), 10);
        finder.findAllPaths(startStation, endStation).forEach(PathFindingTest::printPath);
    }

    private static void printPath(final Path path) {
        final String pathString = Paths.pathToString(path, new Paths.PathDescriptor<Path>() {
            @Override
            public String nodeRepresentation(final Path propertyContainers, final Node node) {
                return node.getProperty("name").toString();
            }

            @Override
            public String relationshipRepresentation(final Path propertyContainers, final Node node, final Relationship relationship) {
                return " -- " + relationship.getType().name() + ": " + relationship.getProperty("time") + " --> ";
            }
        });
        System.out.println(pathString);
    }

    private void printGraph() {
        final ResourceIterator<Node> nodes = graphDb.findNodes(trainLabel);

        while (nodes.hasNext()) {
            final Node node = nodes.next();
            System.out.println("Train: " + node.getProperty("name"));
            for (Relationship relationship : node.getRelationships()) {
                System.out.println("\t" + relationship.getStartNode().getProperty("name") + " --> " + relationship.getEndNode().getProperty(
                        "name"));
                System.out.println("\t\t" + relationship.getType().name() + ": " + relationship.getProperty("time"));
            }
        }
    }

    private static void createStop(final Node stationNode, final Node trainNode, final int arrivalTime, final int departureTime) {
        createArrival(stationNode, trainNode, arrivalTime);
        createDeparture(stationNode, trainNode, departureTime);
    }

    private static void createDeparture(final Node stationNode, final Node trainNode, final int time) {
        Relationship relationship = stationNode.createRelationshipTo(trainNode, ConnectionTypes.DEPARTURE);
        relationship.setProperty("time", time);
    }

    private static void createArrival(final Node stationNode, final Node trainNode, final int time) {
        Relationship relationship = trainNode.createRelationshipTo(stationNode, ConnectionTypes.ARRIVAL);
        relationship.setProperty("time", time);
    }

    private static Node createNode(final Label type, final int number) {
        final Node node = graphDb.createNode(type);
        node.setProperty("name", type.name() + number);
        return node;
    }
}
