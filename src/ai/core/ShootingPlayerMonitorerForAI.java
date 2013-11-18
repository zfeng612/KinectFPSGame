/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.core;

import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;

/**
 *
 * @author Peter Purwanto
 */
public class ShootingPlayerMonitorerForAI extends ShootingPlayerMonitorer{
    
    private Geometry AIBody;
    public ShootingPlayerMonitorerForAI(Camera playerCamera,Geometry AIBody){
        super(playerCamera);
        this.AIBody = AIBody;
    }
    
    /**
     * Specific case that uses the AI bodyGeom's location,
     * to be passed to the general getDistanceToPlayerAim(srcPointLoc) method.
     */
//    private float getDistanceToPlayerAim(){
//        return getDistanceToPlayerAim(movingAIInstance.getLocation());
//    }
}
