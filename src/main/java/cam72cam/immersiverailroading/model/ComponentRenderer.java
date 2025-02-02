package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.mod.render.OpenGL;
import cam72cam.mod.render.obj.OBJVBO.BoundOBJVBO;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ComponentRenderer implements Closeable {
    private final BoundOBJVBO vbo;
    private final List<ModelComponent> buffer = new ArrayList<>();
    private final List<ModelComponentType> available;
    private final OpenGL.With matrix;

    public ComponentRenderer(BoundOBJVBO vbo, List<ModelComponentType> available) {
        this.vbo = vbo;
        this.available = available;
        matrix = OpenGL.matrix();
    }

    public void render(ModelComponent component) {
        if (component != null && (available == null || available.remove(component.type))) {
            buffer.add(component);
        }
    }

    public void render(ModelComponent ... components) {
        for (ModelComponent component : components) {
            render(component);
        }
    }

    public void render(Collection<ModelComponent> components) {
        for (ModelComponent component : components) {
            render(component);
        }
    }

    public ComponentRenderer push() {
        return new ComponentRenderer(vbo, available);
    }

    @Override
    public void close() {
        if (buffer.size() == 1) {
            vbo.draw(buffer.get(0).modelIDs);
        } else if (buffer.size() > 1) {
            vbo.draw(buffer.stream().flatMap(x -> x.modelIDs.stream()).collect(Collectors.toList()));
        }
        matrix.restore();
    }
}
