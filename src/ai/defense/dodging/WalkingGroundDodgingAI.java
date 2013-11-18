package ai.defense.dodging;

import ai.movements.animated.WalkingGroundMovingAI;
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
 * of a ground moving AI. In addition, walking animations are applied while
 * dodging and stopped/standing when not. Best suited for ground-bound AIs with
 * a standard walking animation, such as jMonkey's Oto.
 */
public class WalkingGroundDodgingAI extends GroundDodgingAI{     

    WalkingGroundMovingAI walkingGroundMovingAIInstance;
            
    public WalkingGroundDodgingAI(Spatial bodySpatial,Geometry bodyGeom,Node rootNode,PhysicsSpace physicsSpace,Camera playerCamera){
        super(bodySpatial,bodyGeom,rootNode,physicsSpace,playerCamera);
        this.walkingGroundMovingAIInstance = new WalkingGroundMovingAI(bodySpatial, bodyGeom, rootNode, physicsSpace);
        this.movingAIInstance = walkingGroundMovingAIInstance;
    }
    
    @Override
    protected boolean dodgeIsAppropriate(){
        boolean appropriateResult = super.dodgeIsAppropriate();
            if (!appropriateResult){
                doStandAnimation();
            }
        return appropriateResult;
    }
    @Override
    protected boolean dodgeToBestLocation(){
        boolean dodgeResult = super.dodgeToBestLocation();
            if (!dodgeResult){
                doStandAnimation();
            }
        return dodgeResult;
    }
    
    @Override
    protected void setDodgeMoveRateToForcedRate() {
        super.setDodgeMoveRateToForcedRate();
        walkingGroundMovingAIInstance.setWalkAnimationSpeedToFast();
    }
    
    @Override
    protected void setDodgeMoveRateToBeforeForcedRate() {
        super.setDodgeMoveRateToBeforeForcedRate();
        walkingGroundMovingAIInstance.setWalkAnimationSpeedToDefault();
    }
    
    private void doStandAnimation(){
        walkingGroundMovingAIInstance.doStandAnimation();
    }
}
