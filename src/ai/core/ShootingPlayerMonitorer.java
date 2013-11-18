package ai.core;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import jme3test.helloworld.FPSGame;

/**
 *
 * @author Peter Purwanto
 */
public class ShootingPlayerMonitorer {
    protected Camera playerCamera;

    public Camera getPlayerCamera() {
        return playerCamera;
    }
    
    public ShootingPlayerMonitorer(Camera playerCamera){
        this.playerCamera = playerCamera;
    }
    
    protected Vector3f getPlayerLocation(){
        return playerCamera.getLocation();
    }
    protected Vector3f getPlayerAim(){
        return playerCamera.getDirection();
    }
    
    
    
    /**
     * @param srcPointLoc Gets distance from this given point location
     * to the player camera's location.
     */
    protected float getDistanceToPlayerLocation(Vector3f srcPointLoc){
        return srcPointLoc.distance(getPlayerLocation());
    }
    
    
    
    /**
     * @param srcPointLoc Gets distance from this given point location
     * to the player camera's projected aim/direction.
     */
    protected float getDistanceToPlayerAim(Vector3f srcPointLoc){
        // Variables prefixes used here show Professor Rolf's
        // original Math formula (except srcPointLoc, which had a prefix of P),
        // and the meaning of the variables follow the prefixes, separated by _
        
        // Normalize so the vector magnitude/length is exactly 1
        Vector3f Vc_playerCamDir = getPlayerAim().normalize(),
                 C_playerCamLoc = getPlayerLocation(),
                 V1_playerCamLocToSrcPointLocDist = srcPointLoc.subtract(C_playerCamLoc);
        
        float d_playerCamLocToProjectionLocDist = V1_playerCamLocToSrcPointLocDist.dot(Vc_playerCamDir);
        
        // The projection point that gives the distance
        // from player camera to the source point location when the point is as if it's
        // right in front of the camera
        // (e.g.: Same x- and y-axis, just different z-axis).
        Vector3f Pp_projectionLoc = C_playerCamLoc.add(Vc_playerCamDir.mult(d_playerCamLocToProjectionLocDist));
        
        // For live debugging
        // TODO: The math seems to have a bug! Differences shown by running below code are not always 0... but still seems to make the AI move at the right time in general
//        System.out.println("Projection X Difference to Player Cam. Loc. = "+(C_playerCamLoc.x - Pp_projectionLoc.x)+" | Projection Z Difference to Enemy Loc. = "+(srcPointLoc.z - Pp_projectionLoc.z));
        
        // TODO: Maybe replace all of the .subtract() calls to find distances with .distance(), so that returned values will always be absolute (non-negative), and then no need to do Math.abs() on the resulting distance.
        
        return Math.abs(srcPointLoc.subtract(Pp_projectionLoc).length());
    }
    
// TODO: USE RAYS AS WELL for checking dist. to player aim:
//    public float getRayDistanceToPlayerAim(){
//        CollisionResults aimResults = new CollisionResults();
//        bodyGeom.collideWith(new Ray(getPlayerLocation(), getPlayerAim()), aimResults);
//            if (aimResults.size() > 0) { // Hit something
//
//                // For each hit, we know distance, impact point, name of geometry.
//                CollisionResult closestAim = aimResults.getClosestCollision();
//                return closestAim.getDistance();
//            }
//    }
    
    protected static boolean withinPlayerShootingRange(float distance) {
        return (distance <= FPSGame.getMaxShootRange());
    }
}
