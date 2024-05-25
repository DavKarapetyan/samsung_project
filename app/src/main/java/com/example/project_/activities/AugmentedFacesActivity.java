package com.example.project_.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.example.project_.R;
import com.google.ar.core.AugmentedFace;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Sceneform;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.RenderableInstance;
import com.google.ar.sceneform.rendering.Texture;
import com.google.ar.sceneform.ux.ArFrontFacingFragment;
import com.google.ar.sceneform.ux.AugmentedFaceNode;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class AugmentedFacesActivity extends AppCompatActivity {

    private final Set<CompletableFuture<?>> loaders = new HashSet<>();

    private ArFrontFacingFragment arFragment;
    private ArSceneView arSceneView;

    private Texture faceTexture;
    private ModelRenderable faceModel;

    private final HashMap<AugmentedFace, AugmentedFaceNode> facesNodes = new HashMap<>();

    RoundedImageView foxBtn;
    RoundedImageView faceBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_augmented_faces);

        foxBtn = findViewById(R.id.fox);
        faceBtn = findViewById(R.id.face);

        getSupportFragmentManager().addFragmentOnAttachListener(this::onAttachFragment);

        if (savedInstanceState == null) {
            if (Sceneform.isSupported(this)) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.arFragment, ArFrontFacingFragment.class, null)
                        .commit();
            }
        }

        foxBtn.setOnClickListener(v -> {
            loadModels("models/fox.glb");
            loadTextures("textures/freckles.png");
        });

        faceBtn.setOnClickListener(v -> {
            loadModels("models/face.glb");
            loadTextures("textures/face.png");
        });
    }

    public void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment fragment) {
        if (fragment.getId() == R.id.arFragment) {
            arFragment = (ArFrontFacingFragment) fragment;
            arFragment.setOnViewCreatedListener(this::onViewCreated);
        }
    }

    public void onViewCreated(ArSceneView arSceneView) {
        this.arSceneView = arSceneView;
        arSceneView.setCameraStreamRenderPriority(Renderable.RENDER_PRIORITY_FIRST);
        arFragment.setOnAugmentedFaceUpdateListener(this::onAugmentedFaceTrackingUpdate);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (CompletableFuture<?> loader : loaders) {
            if (!loader.isDone()) {
                loader.cancel(true);
            }
        }
    }

    private void loadModels(String modelName) {
        CompletableFuture<Void> modelLoader = ModelRenderable.builder()
                .setSource(this, Uri.parse(modelName))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(model -> {
                    faceModel = model;
                    updateFaceNodes();
                })
                .exceptionally(throwable -> {
                    Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG).show();
                    return null;
                });
        loaders.add(modelLoader);
    }

    private void loadTextures(String textureName) {
        CompletableFuture<Void> textureLoader = Texture.builder()
                .setSource(this, Uri.parse(textureName))
                .setUsage(Texture.Usage.COLOR_MAP)
                .build()
                .thenAccept(texture -> {
                    faceTexture = texture;
                    updateFaceNodes();
                })
                .exceptionally(throwable -> {
                    Toast.makeText(this, "Unable to load texture", Toast.LENGTH_LONG).show();
                    return null;
                });
        loaders.add(textureLoader);
    }

    private void updateFaceNodes() {
        if (faceModel != null && faceTexture != null) {
            for (AugmentedFaceNode faceNode : facesNodes.values()) {
                RenderableInstance modelInstance = faceNode.setFaceRegionsRenderable(faceModel);
                modelInstance.setShadowCaster(false);
                modelInstance.setShadowReceiver(true);
                faceNode.setFaceMeshTexture(faceTexture);
            }
        }
    }

    public void onAugmentedFaceTrackingUpdate(AugmentedFace augmentedFace) {
        if (faceModel == null || faceTexture == null) {
            return;
        }

        AugmentedFaceNode existingFaceNode = facesNodes.get(augmentedFace);

        switch (augmentedFace.getTrackingState()) {
            case TRACKING:
                if (existingFaceNode == null) {
                    AugmentedFaceNode faceNode = new AugmentedFaceNode(augmentedFace);
                    RenderableInstance modelInstance = faceNode.setFaceRegionsRenderable(faceModel);
                    modelInstance.setShadowCaster(false);
                    modelInstance.setShadowReceiver(true);
                    faceNode.setFaceMeshTexture(faceTexture);
                    arSceneView.getScene().addChild(faceNode);
                    facesNodes.put(augmentedFace, faceNode);
                }
                break;
            case STOPPED:
                if (existingFaceNode != null) {
                    arSceneView.getScene().removeChild(existingFaceNode);
                }
                facesNodes.remove(augmentedFace);
                break;
        }
    }
}
