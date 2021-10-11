/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PROP.robot;

import robocode.HitByBulletEvent;
import robocode.Robot;
import robocode.ScannedRobotEvent;

/**
 *
 * @author srimp
 */
public class TorreRobot extends Robot {
    private static double bearingThreshhold = 5;
    
    public void run () {
        turnLeft(getHeading());
    }
    
    double normalizeBearing (double bearing) {
        while (bearing > 180) bearing -= 360;
        while (bearing < -180) bearing += 360;
        return bearing;
    }
    
    @Override
    public void onScannedRobot (ScannedRobotEvent e) {
        if (normalizeBearing(e.getBearing()) < bearingThreshhold) {
            fire(1);
        }
    }
    
    @Override
    public void onHitByBullet (HitByBulletEvent e) {
        // turnLeft(180);
    }
}
