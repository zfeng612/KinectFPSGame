package ai.attack.shooting;

import ai.core.ShootingPlayerMonitorer;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Sphere;
import jme3test.helloworld.BombControl;

/**
 *
 * @author Peter Purwanto
 * 
 * 
 */
public class ShootingAI extends ShootingPlayerMonitorer{
    
//    public static final 
    private static final Sphere ballSphere;
    private Material enemyBombMat;
    private Geometry bodyGeom;
    private AssetManager assetManager;
    private Node rootNode;
    private PhysicsSpace physicsSpace;
    
    public static final long ATTACK_DELAY_MILLIS = 3000;
    private long lastAttackWaitStartTimeMillis = Long.MAX_VALUE;
    
    static { // This is the static/global "Constructor", usually used to initialize static variables.
        /**
         * Initialize the cannon ball geometry
         */
        ballSphere = new Sphere(32, 32, 1f, true, false);
        ballSphere.setTextureMode(Sphere.TextureMode.Projected);

    }
    
    /**
     * @param playerCamera The player's camera, whose position is where the AI should shoot at.
     */
    public ShootingAI(Camera playerCamera,Geometry bodyGeom,AssetManager assetManager,Node rootNode,PhysicsSpace physicsSpace){
        super(playerCamera);
        this.bodyGeom = bodyGeom;
        this.assetManager = assetManager;
        this.rootNode = rootNode;
        this.physicsSpace = physicsSpace;
        
        enemyBombMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        enemyBombMat.setColor("Color", ColorRGBA.Blue);
        enemyBombMat.setColor("GlowColor", ColorRGBA.Blue);
    }
    
    private void shootAtPlayer() {
        
        Geometry bombGeo = new Geometry("cannon ball", ballSphere);
        bombGeo.setMaterial(enemyBombMat);

        Vector3f curBodyLoc = getBodyLocation();
        bombGeo.setLocalTranslation(curBodyLoc.x, curBodyLoc.y - 5, curBodyLoc.z);
//        bombGeo.lookAt(getBodyDirection(),Vector3f.UNIT_Y);

        SphereCollisionShape bombCollisionShape = new SphereCollisionShape(1f);
        RigidBodyControl bombNode = new BombControl(assetManager, bombCollisionShape, 1);

        Vector3f curPlayerLoc = getPlayerLocation();
        Vector3f bombTargetLoc = new Vector3f(curPlayerLoc.x + 20,curPlayerLoc.y - 45,curPlayerLoc.z);

        bombNode.setLinearVelocity(bombTargetLoc); //bodyGeom.localToWorld(new Vector3f(0, 0, 1f), null).subtract(curPlayerLoc)

        bombGeo.addControl(bombNode);
        rootNode.attachChild(bombGeo);
        physicsSpace.add(bombNode);
    }
    
    private Vector3f getBodyLocation(){
        return bodyGeom.getLocalTranslation();
    }
    
    private Vector3f getBodyDirection(){
        return bodyGeom.getLocalRotation().getRotationColumn(2);
    }
    
    /**
     * *** THIS IS THE MAIN LOGIC & ACTION PERFORMED BY THE SHOOTING AI
     * AND SHOULD BE CALLED/POLLED CONTINUOUSLY IN simpleUpdate()
     */
    public boolean shootingIsAppropriate(){
        
            // If AI is within player shooting range for too long, shoot.
            if (withinPlayerShootingRange(getDistanceToPlayerLocation(getBodyLocation()))){
                    if (lastAttackWaitStartTimeMillis == Long.MAX_VALUE){
                        lastAttackWaitStartTimeMillis = System.currentTimeMillis();
                    }
                    
                    if (System.currentTimeMillis() < (lastAttackWaitStartTimeMillis+ATTACK_DELAY_MILLIS)){
                        return false;
                    }
                    else{
                        lastAttackWaitStartTimeMillis = Long.MAX_VALUE;
                        return true;
                    }
            }
            else{
                // As soon as AI is no longer within the range, reset timer.
                lastAttackWaitStartTimeMillis = Long.MAX_VALUE;
                return false;
            }
    }
    
    public void shootWhenAppropriate(){
        if (shootingIsAppropriate()){
            shootAtPlayer();
        }
    }
}
