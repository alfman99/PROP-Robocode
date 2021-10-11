package PROP.robot;

import robocode.HitWallEvent;
import robocode.HitRobotEvent;
import robocode.HitByBulletEvent;
import robocode.ScannedRobotEvent;
import java.awt.Color;
import robocode.AdvancedRobot;

public class RobotOP extends AdvancedRobot
{
    private static final int TurretMode_SCAN = 1;
    private static final int TurretMode_AIM = 2;
    private static final int TurretMode_BACK = 3;
    private long endTimeTurretMode;
    private double turretRotationSense;
    double tankHeading;
    long ticks;
    int turretMode;
    double aimToHeading;
    boolean isTurning;
    long endTurningTick;
    long TURNING_TICKS;
    
    public RobotOP() {
        this.turretRotationSense = 1.0;
        this.tankHeading = 90.0;
        this.ticks = 0L;
        this.turretMode = 1;
        this.aimToHeading = 0.0;
        this.isTurning = false;
        this.endTurningTick = 0L;
        this.TURNING_TICKS = 10L;
    }
    
    private void setTurretModeAndTime(final int turretMode, final int n) {
        if (this.turretMode == 3) {
            return;
        }
        this.turretMode = turretMode;
        this.endTimeTurretMode = this.ticks + n;
    }
    
    private void checkTurretModeEndTime(final int turretMode) {
        if (this.ticks > this.endTimeTurretMode) {
            this.turretMode = turretMode;
        }
    }
    
    private double clamp360(double n) {
        final int n2 = (n >= 0.0) ? 1 : -1;
        n *= n2;
        if (n >= 360.0) {
            n -= 360 * (int)n / 360;
        }
        return n * n2;
    }
    
    private double sr(double n) {
        if (n >= 180.0) {
            n = 360.0 - n;
        }
        if (n <= -180.0) {
            n += 360.0;
        }
        return n;
    }
    
    @Override
    public void run() {
        this.setColors(Color.red, Color.blue, Color.green);
        while (true) {
            if (this.isTurning) {
                if (this.ticks >= this.endTurningTick) {
                    this.isTurning = false;
                }
            }
            else {
                if (this.ticks % 20L == 0L) {
                    this.tankHeading += 90.0;
                    this.tankHeading = this.clamp360(this.tankHeading);
                }
                this.checkTurretModeEndTime(1);
            }
            this.setTurnRight(this.sr(this.tankHeading - this.getHeading()));
            if (this.turretMode == 1) {
                this.setAhead(200.0);
                this.setTurnGunRight(this.sr(10.0 * this.turretRotationSense));
            }
            else if (this.turretMode == 2) {
                this.setAhead(200.0);
                this.tankHeading += ((this.ticks % 3L < this.ticks % 6L) ? 20 : -20);
                this.setTurnGunRight(this.sr(this.aimToHeading - this.getGunHeading()));
            }
            else if (this.turretMode == 3) {
                this.setAhead(-200.0);
            }
            this.execute();
            ++this.ticks;
        }
    }
    
    @Override
    public void onScannedRobot(final ScannedRobotEvent scannedRobotEvent) {
        final double x = this.getX();
        final double y = this.getY();
        final double clamp360 = this.clamp360(scannedRobotEvent.getBearing() + this.getHeading());
        final double n = x + scannedRobotEvent.getDistance() * Math.cos(Math.toRadians(clamp360));
        final double n2 = y + scannedRobotEvent.getDistance() * Math.sin(Math.toRadians(clamp360));
        final double n3 = 50.0;
        final double n4 = scannedRobotEvent.getVelocity() * n3 * Math.cos(scannedRobotEvent.getHeadingRadians());
        final double n5 = scannedRobotEvent.getVelocity() * n3 * Math.sin(scannedRobotEvent.getHeadingRadians());
        this.turretRotationSense = (((n - this.getX()) * n5 - (n2 - this.getY()) * n4 >= 0.0) ? 1 : -1);
        final double degrees = Math.toDegrees(Math.atan2(n2 + n5 - this.getY(), n + n4 - this.getX()));
        if (!this.isTurning) {
            this.tankHeading = degrees;
        }
        if (this.getGunHeat() == 0.0) {
            if (scannedRobotEvent.getDistance() < 60.0) {
                this.fire(3.0);
            }
            else if (scannedRobotEvent.getDistance() < 400.0) {
                this.fire(2.5);
            }
            else if (scannedRobotEvent.getDistance() < 600.0) {
                this.fire(1.0);
            }
        }
        this.setTurretModeAndTime(2, 30);
        this.aimToHeading = degrees;
    }
    
    @Override
    public void onHitByBullet(final HitByBulletEvent hitByBulletEvent) {
        this.turretMode = 2;
        this.aimToHeading = this.clamp360(this.getHeading() + hitByBulletEvent.getBearing());
        this.tankHeading = this.aimToHeading;
    }
    
    @Override
    public void onHitRobot(final HitRobotEvent hitRobotEvent) {
    }
    
    @Override
    public void onHitWall(final HitWallEvent hitWallEvent) {
        if (!this.isTurning) {
            this.isTurning = true;
            this.endTurningTick = this.ticks + this.TURNING_TICKS;
            this.tankHeading = this.clamp360(this.getHeading() + hitWallEvent.getBearing() + 180.0);
        }
    }
}
