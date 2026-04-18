import org.mapsforge.map.rendertheme.InternalRenderTheme;
public class TestMapsforgeTheme {
    public static void main(String[] args) {
        for (InternalRenderTheme theme : InternalRenderTheme.values()) {
            System.out.println(theme.name());
        }
    }
}
