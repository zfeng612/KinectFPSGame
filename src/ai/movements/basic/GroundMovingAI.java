package ai.movements.basic;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 *
 * @author Peter Purwanto
 * 
 * This class is specific for GROUND moving AI's tasks, and disables
 * flying and other tasks unused by ground-only AI.
 * When instantiated, the provided Spatial/Geometry moving physics is
 * also automatically set up (using Kinematic RigidBodyControl)!
 */
public class GroundMovingAI extends MovingAI{
    // TODO: Maybe upon instantiation or provide a method to automatically put the AI close to the ground/simulate gravity effect
    public GroundMovingAI(Spatial bodySpatial,Geometry bodyGeom,Node rootNode,PhysicsSpace physicsSpace){
        super(bodySpatial, bodyGeom, rootNode, physicsSpace);
        
        this.bodyPhysics = bodyGeom.getControl(RigidBodyControl.class);
            if (bodyPhysics == null){
                bodyPhysics = new RigidBodyControl(0.0f);
                bodyGeom.addControl(bodyPhysics);
                physicsSpace.add(bodyPhysics);
            }
            else{
                bodyPhysics.setEnabled(true);
            }
        
        // Since this is a moving AI, we need to always update its
        // RigidBodyControl; this is easily done by enabling kinematic.
        // However, MY GUESS is this MIGHT also mean the AI won't be affected by gravity either.
        bodyPhysics.setKinematic(true);
        
        
//            if (physicsSpace != null){
//                // Create a appropriate physical shape for it
//                CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(4f,6f,1);
//                this.bodyPhysics = new CharacterControl(capsuleShape, 0.05f);
//                // Attach physical properties to model and PhysicsSpace
//                bodyGeom.addControl(bodyPhysics);
//                physicsSpace.add(bodyPhysics);
//            }
    }
    
    
    
    @Override
    public void flyUp(float amountInMeters) { /* Ground AI Can't Fly */ }
    
    @Override
    public Vector3f getProjectedFlyUpVector(float amountInMeters){
        return null;  /* Ground AI Can't Fly */ 
    }
    
    
    
    @Override
    public void flyDown(float amountInMeters) { /* Ground AI Can't Fly */ }
    
    @Override
    public Vector3f getProjectedFlyDownVector(float amountInMeters){
        return null;  /* Ground AI Can't Fly */ 
    }
    
    
    
    /**
     * Safe to move the AI body to specified target location only if,
     * at target location, there will be no obstacle in front of or cliff below
     * the AI body/feet.
     */
    public boolean safeToMove(Vector3f targetLoc,Quaternion straightFacingRotation){
        return (!obstacleCloseInFront(targetLoc,MovingAI.getDirectionFromRotation(straightFacingRotation),rootNode) && !cliffBelow(targetLoc,straightFacingRotation,rootNode));
    }
}
