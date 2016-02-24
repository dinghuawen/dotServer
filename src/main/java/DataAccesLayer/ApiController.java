package DataAccesLayer;

import Datastructures.MultiResTree;
import Datastructures.MultiResolutionNode;
import Datastructures.OctreeNode;
import Datastructures.Raster;
import fi.iki.elonen.NanoHTTPD;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.la4j.Vector;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ApiController extends NanoHTTPD {

    MultiResTree mrt = new MultiResTree();
    Map<String, OctreeNode> map = new HashMap<>();

    public ApiController(int port, MultiResTree mrt) throws IOException {
        super(port);
        this.mrt = mrt;
        map = mrt.index;
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("\nRunning! Point your browers to http://localhost:8080/ \n Keep going Jakob!");
    }

    @Override
    public Response serve(IHTTPSession session) {
        Map<String, String> params = session.getParms();
        switch (params.get("mode")){
            case "tree":
                byte[] protobuf = buildMRTProto(mrt).toByteArray();
                return  NanoHTTPD.newFixedLengthResponse(Response.Status.OK,"application/octet-stream",
                        new ByteArrayInputStream(protobuf), protobuf.length);
            case "samples":
                String key =  params.get("id");
                if (map.containsKey(key)){
                    //return NanoHTTPD.newFixedLengthResponse(map.get(key).toString());
                    MultiResolutionNode n = (MultiResolutionNode) map.get(key);
                    byte[] protobufRaster = buildRasterProto(n.raster).toByteArray();
                    return  NanoHTTPD.newFixedLengthResponse(Response.Status.OK,"application/octet-stream",
                            new ByteArrayInputStream(protobufRaster), protobufRaster.length);
                }
                return NanoHTTPD.newFixedLengthResponse("Invalid Parameters \n depth:" + params.get("depth")
                        + "\n node:" + params.get("node"));
            default:
                return NanoHTTPD.newFixedLengthResponse("Invalid Mode");
        }
    }

    static private MultiResTreeProtos.MRTree buildMRTProto(MultiResTree mrt){
        MultiResTreeProtos.MRTree.Builder b = MultiResTreeProtos.MRTree.newBuilder();
        b.setRoot(_buildMRTProto(MultiResTreeProtos.MRTree.MRNode.newBuilder(), mrt.getRoot()));
        return b.build();
    }

    static private MultiResTreeProtos.MRTree.MRNode _buildMRTProto( MultiResTreeProtos.MRTree.MRNode.Builder b,
                                                                    OctreeNode node){
        b.setId(node.id);
        b.setCellLength(node.cellLength);
        b.setPointCount(node.pointCount);
        b.setIsLeaf(node.isLeaf);
        b.addAllCenter(node.center);

        if (!node.isLeaf) {
            for (int i = 0; i < 8; i++){
                b.addOctant(i, _buildMRTProto(MultiResTreeProtos.MRTree.MRNode.newBuilder(), node.octants[i]));
            }
        }

        return b.build();
    }

    static private RasterProtos.Raster buildRasterProto(Raster r){
        RasterProtos.Raster.Builder b = RasterProtos.Raster.newBuilder();
        for (Triplet<double[], int[], Integer> t: r){
            RasterProtos.Raster.Point3DRGB.Builder bP = RasterProtos.Raster.Point3DRGB.newBuilder();
            for (int i = 0; i < 3; i++) {
                bP.addPosition(t.getValue0()[i]);
                bP.addColor(t.getValue1()[i]);
            }
            bP.setSize(t.getValue2());
            b.addSample(bP);
        }
        return b.build();
    }



}