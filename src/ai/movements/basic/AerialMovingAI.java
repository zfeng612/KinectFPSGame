package ai.movements.basic;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 *
 * @author Peter Purwanto
 * 
 * This class is specific for FLYING AI's tasks.
 * TODO: For now, when instantiated, the provided Geometry's physics
 * will NOT be set up, since this is a flying AI and will be like a ghost.
 * It'll still avoid bumping to obstacles, though.
 */
public class AerialMovingAI extends MovingAI{
    
    public static final float MIN_ALLOWED_DISTANCE_ABOVE_GROUND = 1f;

    public AerialMovingAI(Spatial bodySpatial,Geometry bodyGeom,Node rootNode,PhysicsSpace physicsSpace){
        super(bodySpatial, bodyGeom, rootNode, physicsSpace);
    }
    
    
    
    @Override
    public void flyUp(float amountInMeters) {  /* For now, AI Can't Change Fly Direction */ }
    
    @Override
    public Vector3f getProjectedFlyUpVector(float amountInMeters){
        return null;  /* For now, AI Can't Change Fly Direction */
    }
    
    
    
    @Override
    public void flyDown(float amountInMeters) {  /* For now, AI Can't Change Fly Direction */  }
    
    @Override
    public Vector3f getProjectedFlyDownVector(float amountInMeters){
        return null;  /* For now, AI Can't Change Fly Direction */
    }
    
    
    
    /**
     * Safe to move the AI body to specified target location only if,
     * at target location, there will be no obstacle in front of the AI body
     * (but since this is flying AI, cliffs below are OK).
     */
    public boolean safeToMove(Vector3f targetLoc,Quaternion straightFacingRotation){
        return (!obstacleCloseInFront(targetLoc,MovingAI.getDirectionFromRotation(straightFacingRotation),rootNode) && (getDistanceToGround(targetLoc,straightFacingRotation,rootNode) > MIN_ALLOWED_DISTANCE_ABOVE_GROUND));
    }
}
