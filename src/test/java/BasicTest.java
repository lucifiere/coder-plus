import com.dlin.bootstrap.DdlCodeGenerator;
import com.dlin.templates.embed.BaseModelTemplatesConfig;
import com.dlin.templates.embed.EmbedTemplates;
import org.junit.Test;

/**
 * @author created by XD.Wang
 * Date 2020/9/4.
 */
public class BasicTest {

    @Test
    public void testBootstrap() {
        DdlCodeGenerator myBootstrap = new DdlCodeGenerator();
        myBootstrap.setDdlName("ddl.sql")
                .setWorkspacePath("/Users/wangxiandui/Documents/gen-codes");
        myBootstrap.execute(EmbedTemplates.MODEL);
    }

}
