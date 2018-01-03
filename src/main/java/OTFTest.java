import index.OTFSearcher;
import java.io.IOException;
import java.util.Set;

public class OTFTest {

    public static  void main(String args[]) throws IOException{
        OTFSearcher otf = new OTFSearcher(args[0]);
        String [] input = {"politics of jordan"};
        Set<String> answers = otf.urisToEntityName(input);
        System.out.println("URIs found:");
        for(String uri : answers){
            System.out.println(uri);
        }
    }
}
