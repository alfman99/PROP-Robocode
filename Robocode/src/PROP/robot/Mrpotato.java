package PROP.robot;

import robocode.*;

/**
 *
 * @author srimp
 */
public class Mrpotato extends AdvancedRobot {
    @Override
    public void run() {
     // Inicializaciones
     
     while(true) {
         scan();
         execute();
     }   
    }
    public void onScannedRobot(ScannedRobotEvent e) {
        analiza(e);
        esquivar(e);
        circularTarget(e);
        apuntaYDisparar(e);
    }
    public void onHitWall(HitWallEvent e) {
        
    }
    public void onHitByBullet(HitByBulletEvent e) {
        
    }
    public void onBulletHit(BulletHitEvent e) {
        
    }
    /**
     * Metodo que analiza la situacion en el campo de batalla respecto al enemigo. Ajustamos la orientacion,
     * velocidad y el movimiento del radar y del cañón
     * @param e Permite obtener información del enemigo
     */
    private void analiza(ScannedRobotEvent e) {
        
    }
    /**
     * Método usado para identificar la posición del enemigo a través de una política de target circular
     * @param e 
     */
    private void circularTarget(ScannedRobotEvent e) {
        
    }
    /**
     * Método que permite al tanque apuntar y disparar a la posición donde predecimos que estará el enemigo.
     * @param e Permite obtener información del enemigo
     */
    private void apuntaYDisparar(ScannedRobotEvent e) {
        
    }
    /**
     * Método que permite al robot esquivar un disparo.
     * @param e 
     */
    private void esquivar(ScannedRobotEvent e) {
        
    }
    
}
