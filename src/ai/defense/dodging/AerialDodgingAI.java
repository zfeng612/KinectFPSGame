package ai.defense.dodging;

import ai.movements.basic.AerialMovingAI;
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
 * of a flying AI. Best suited for air-bound AIs.
 */
public class AerialDodgingAI extends DodgingAI{     

// TODO: Make AerialDodgingAI have limited fly up/down moves so that it won't go out of player's shoot range.
//    private class AerialRestrictedFylingHeightAI extends AerialMovingAI{
//        private AerialRestrictedFylingHeightAI(Spatial bodySpatial,Geometry bodyGeom,Node rootNode,PhysicsSpace physicsSpace){
//            super(bodySpatial, bodyGeom, rootNode, physicsSpace);
//        }   
//    }
//    this.movingAIInstance = new AerialRestrictedFylingHeightAI(bodySpatial, bodyGeom, rootNode, physicsSpace);
    
    public AerialDodgingAI(Spatial bodySpatial,Geometry bodyGeom,Node rootNode,PhysicsSpace physicsSpace,Camera playerCamera){
        super(playerCamera);
        this.movingAIInstance = new AerialMovingAI(bodySpatial, bodyGeom, rootNode, physicsSpace);
    }
}
