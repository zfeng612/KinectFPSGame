package ai.defense.dodging;

import ai.movements.basic.GroundMovingAI;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 *
 * @author Peter Purwanto
 * 
 * Instances of this class will make the dodging AI use the movement mechanism
 * of a ground moving AI. Best suited for ground-bound AIs.
 */
public class GroundDodgingAI extends DodgingAI{     

    public GroundDodgingAI(Spatial bodySpatial,Geometry bodyGeom,Node rootNode,PhysicsSpace physicsSpace,Camera playerCamera){
        super(playerCamera);
        this.movingAIInstance = new GroundMovingAI(bodySpatial, bodyGeom, rootNode, physicsSpace);
    }
}
