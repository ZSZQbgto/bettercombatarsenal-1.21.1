package com.zszq.neonarsenal.client;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.math.Transformation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.model.IModelBuilder;
import net.neoforged.neoforge.client.model.SimpleModelState;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.SimpleUnbakedGeometry;
import net.neoforged.neoforge.client.model.geometry.UnbakedGeometryHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Geometry loader that reads an <b>existing</b> vanilla block-model JSON and accepts both of the
 * element-rotation encodings used by newer Blockbench exports:
 *
 * <ul>
 *   <li>{@code {"origin": [...], "axis": "z", "angle": 15}} — supported by 1.21.1, but with any
 *       angle (the vanilla deserializer only allows {@code -45/-22.5/0/22.5/45});</li>
 *   <li>{@code {"origin": [...], "x": 180, "y": 0, "z": 155}} — arbitrary Euler angles, which
 *       1.21.1's {@link BlockElementRotation} cannot represent at all.</li>
 * </ul>
 *
 * <p>Editing the supplied assets is not allowed, and vanilla's {@code ModelManager} eagerly parses
 * every file under {@code models/}, so such exports are shipped byte-identically under
 * {@code assets/bettercombat_neonarsenal/geometry/}. This loader reads that source through the
 * {@code source} key of our wrapper models and bakes the elements itself: legacy axis/angle
 * rotations use {@link BlockElementRotation}, Euler rotations are converted to a quaternion and
 * baked through a per-element {@link ModelState} (rotating about the element's own origin). The
 * result matches the authored geometry exactly.</p>
 */
public final class PermissiveModelLoader implements IGeometryLoader<PermissiveModelLoader.Geometry> {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            com.zszq.neonarsenal.NeonArsenal.MODID, "permissive_model");

    private static final Gson ELEMENT_GSON = new GsonBuilder()
            .registerTypeAdapter(BlockElementFace.class, new BlockElementFace.Deserializer())
            .registerTypeAdapter(BlockFaceUV.class, new BlockFaceUV.Deserializer())
            .create();

    @Override
    public Geometry read(JsonObject json, com.google.gson.JsonDeserializationContext context) throws JsonParseException {
        ResourceLocation source = ResourceLocation.parse(GsonHelper.getAsString(json, "source"));
        JsonObject sourceJson = readSourceFile(source);
        List<ElementData> elements = new ArrayList<>();
        JsonArray elementsJson = GsonHelper.getAsJsonArray(sourceJson, "elements");
        for (JsonElement entry : elementsJson) {
            elements.add(parseElement(entry.getAsJsonObject()));
        }
        if (elements.isEmpty()) {
            throw new JsonParseException("Source model " + source + " has no elements");
        }
        return new Geometry(elements);
    }

    private static JsonObject readSourceFile(ResourceLocation source) {
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath(
                source.getNamespace(), source.getPath() + ".json");
        Resource resource = Minecraft.getInstance().getResourceManager().getResource(file)
                .orElseThrow(() -> new JsonParseException("Missing source model file: " + file));
        try (Reader reader = resource.openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            throw new JsonParseException("Failed to read source model file: " + file, e);
        }
    }

    private static ElementData parseElement(JsonObject json) {
        Vector3f from = vector(json, "from");
        Vector3f to = vector(json, "to");
        BlockElementRotation legacyRotation = null;
        EulerRotation eulerRotation = null;

        if (json.has("rotation")) {
            JsonObject rotationJson = GsonHelper.getAsJsonObject(json, "rotation");
            Vector3f origin16 = vector(rotationJson, "origin");
            if (rotationJson.has("axis")) {
                Direction.Axis axis = Direction.Axis.byName(
                        GsonHelper.getAsString(rotationJson, "axis").toLowerCase(Locale.ROOT));
                float angle = GsonHelper.getAsFloat(rotationJson, "angle");
                boolean rescale = GsonHelper.getAsBoolean(rotationJson, "rescale", false);
                legacyRotation = new BlockElementRotation(
                        new Vector3f(origin16).mul(0.0625F), axis, angle, rescale);
            } else if (rotationJson.has("x") || rotationJson.has("y") || rotationJson.has("z")) {
                float x = GsonHelper.getAsFloat(rotationJson, "x", 0.0F);
                float y = GsonHelper.getAsFloat(rotationJson, "y", 0.0F);
                float z = GsonHelper.getAsFloat(rotationJson, "z", 0.0F);
                // Blockbench's java_block format keeps Format.euler_order = 'ZYX' (three.js
                // default), i.e. q = qz * qy * qx. Using XYZ here mirrors one of the double axe
                // blades, so the order matters.
                Quaternionf quaternion = new Quaternionf()
                        .rotationZ(z * Mth.DEG_TO_RAD)
                        .rotateY(y * Mth.DEG_TO_RAD)
                        .rotateX(x * Mth.DEG_TO_RAD);
                eulerRotation = new EulerRotation(quaternion, origin16);
            }
        }

        Map<Direction, BlockElementFace> faces = new EnumMap<>(Direction.class);
        JsonObject facesJson = GsonHelper.getAsJsonObject(json, "faces");
        for (Map.Entry<String, JsonElement> entry : facesJson.entrySet()) {
            Direction direction = Direction.byName(entry.getKey());
            if (direction == null) {
                throw new JsonParseException("Unknown facing: " + entry.getKey());
            }
            faces.put(direction, ELEMENT_GSON.fromJson(entry.getValue(), BlockElementFace.class));
        }
        if (faces.isEmpty()) {
            throw new JsonParseException("Element without faces");
        }
        boolean shade = GsonHelper.getAsBoolean(json, "shade", true);
        BlockElement element = new BlockElement(from, to, faces, legacyRotation, shade);
        return new ElementData(element, eulerRotation);
    }

    private static Vector3f vector(JsonObject json, String name) {
        JsonArray array = GsonHelper.getAsJsonArray(json, name);
        if (array.size() != 3) {
            throw new JsonParseException("Expected 3 " + name + " values, found: " + array.size());
        }
        return new Vector3f(
                GsonHelper.convertToFloat(array.get(0), name + "[0]"),
                GsonHelper.convertToFloat(array.get(1), name + "[1]"),
                GsonHelper.convertToFloat(array.get(2), name + "[2]"));
    }

    private record EulerRotation(Quaternionf quaternion, Vector3f origin16) {
    }

    private record ElementData(BlockElement element, EulerRotation eulerRotation) {
    }

    /** Bakes the parsed elements like NeoForge's {@code ElementsModel}, including arbitrary rotations. */
    public static final class Geometry extends SimpleUnbakedGeometry<Geometry> {
        private final List<ElementData> elements;

        private Geometry(List<ElementData> elements) {
            this.elements = elements;
        }

        @Override
        protected void addQuads(IGeometryBakingContext context, IModelBuilder<?> modelBuilder, ModelBaker baker,
                                Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState) {
            Transformation rootTransform = context.getRootTransform();
            if (!rootTransform.isIdentity()) {
                modelState = UnbakedGeometryHelper.composeRootTransformIntoModelState(modelState, rootTransform);
            }
            for (ElementData data : elements) {
                if (data.eulerRotation() == null) {
                    bakeFaces(data.element(), modelState, context, modelBuilder, spriteGetter);
                } else {
                    bakeEulerElement(data, modelState, context, modelBuilder, spriteGetter);
                }
            }
        }

        private static void bakeEulerElement(ElementData data, ModelState modelState, IGeometryBakingContext context,
                                             IModelBuilder<?> modelBuilder,
                                             Function<Material, TextureAtlasSprite> spriteGetter) {
            EulerRotation rotation = data.eulerRotation();
            // Shift the element so its rotation origin sits at the block centre, bake with the
            // quaternion as the model rotation (which rotates about the centre), and let the
            // transformation translation bring it back. This reproduces a rotation about the
            // element's own origin, which BlockElementRotation cannot express for Euler angles.
            Vector3f shift = new Vector3f(8.0F, 8.0F, 8.0F).sub(rotation.origin16(), new Vector3f());
            BlockElement base = data.element();
            BlockElement shifted = new BlockElement(
                    new Vector3f(base.from).add(shift),
                    new Vector3f(base.to).add(shift),
                    base.faces,
                    null,
                    base.shade);
            Vector3f delta = new Vector3f(shift).div(16.0F);
            Transformation transform = new Transformation(
                    new Vector3f(-delta.x(), -delta.y(), -delta.z()),
                    new Quaternionf(rotation.quaternion()),
                    new Vector3f(1.0F, 1.0F, 1.0F),
                    null);
            ModelState rotatedState = new SimpleModelState(transform);
            bakeFaces(shifted, rotatedState, context, modelBuilder, spriteGetter);
        }

        private static void bakeFaces(BlockElement element, ModelState modelState,
                                      IGeometryBakingContext context, IModelBuilder<?> modelBuilder,
                                      Function<Material, TextureAtlasSprite> spriteGetter) {
            for (Direction direction : element.faces.keySet()) {
                BlockElementFace face = element.faces.get(direction);
                TextureAtlasSprite sprite = spriteGetter.apply(context.getMaterial(face.texture()));
                BakedQuad quad = BlockModel.bakeFace(element, face, sprite, direction, modelState);
                if (face.cullForDirection() == null) {
                    modelBuilder.addUnculledFace(quad);
                } else {
                    modelBuilder.addCulledFace(modelState.getRotation().rotateTransform(face.cullForDirection()), quad);
                }
            }
        }
    }
}
