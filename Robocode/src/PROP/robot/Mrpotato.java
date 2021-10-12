package PROP.robot;

import java.awt.geom.Point2D;
import robocode.*;

/**
 *
 * @author srimp
 */
public class Mrpotato extends AdvancedRobot {
    //Atributos de la clase
    private Integer dir;
    private double anteriorDirEnemigo;
    private double predX;
    private double predY;
    private double velocidad_bala = 20-3*Rules.MAX_BULLET_POWER;
    public Mrpotato(){
        this.dir = 1;
        this.anteriorDirEnemigo = 0.0;
    }
    @Override
    public void run() {
     // Inicializaciones
        while(true) {
            scan();
            execute();
        }   
    }
    
    
    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        analiza(e);
        esquivar(e);
        circularTarget(e);
        apuntaYDisparar(e);
    }
    
    
    @Override
    public void onHitWall(HitWallEvent e) {
        
    }
    
    
    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        
    }
    
    
    @Override
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
     * Método usado para identificar el punto hacía donde disparar, según la posición del enemigo, a través de una política de target circular
     * @param e 
     */
    private void circularTarget(ScannedRobotEvent e) {
        //Calculamos la diferencia de grados entre la anterior posición del enemigo calculada y la nueva. Excepto en el primer calculo dónde
        //la anterior dirección del enemigo está inicializada en 0.0. Además actualizamos la que será la nueva anterior dirección del enemigo
        double actDirEnemigo = e.getHeading();
        double diferenciaGrados = actDirEnemigo - anteriorDirEnemigo;
        anteriorDirEnemigo = actDirEnemigo;
        
        //Predicción inicial
        //LO QUE VA EN LOS PARENTESIS DE LO DE SIN Y COS SI NO ESTOY PATINANDO ES LA DIRECCION. ASÍ QUE A LO MEJOR ESO HAY QUE GUARDARLO EN UNA VARIABLE Y METERLO EN LA FUNCION DE ANALIZAR
        predX = getX() + e.getDistance()*Math.sin((e.getBearing()+getHeading()));
        predY = getY() + e.getDistance()*Math.cos((e.getBearing()+getHeading()));
        //Queremos que la bala de al enemigo, por tanto calculamos iterativamente predicciones hasta que pueda alcanzarla
        //teniendo en cuenta el tiempo transcurrido en cada iteración
        double tiempo = 0;
        while(Point2D.Double.distance(getX(), getY(), predX, predY) > (tiempo*velocidad_bala)){
            //Actualizamos los valores de las coordenadas predecidas anteriormente teniendo en cuenta la dirección y velocidad del enemigo
            predX = predX + e.getVelocity()*Math.sin(actDirEnemigo);
            predY = predY + e.getVelocity()*Math.cos(actDirEnemigo);
            
            actDirEnemigo = actDirEnemigo + diferenciaGrados;
            //LOS BORDES DEL MAPA HAY QUE TENERLOS AQUI EN CUENTA??
        }
        //ALFREDO PARA QUE CUANDO TE DESCARGUES ESTO ME ENTIENDAS. CREO QUE TENIAS TODA LA RAZÓN DEL MUNDO Y ES MEJOR METER LO DE DISPARAR AQUI
        //PORQUE COMO YA TENGO EN CUENTA LOS TICKS CON LO DE TIEMPO EL MOMENTO DE DISPARAR SERIA AHORA. ASI QUE EL METODO apuntaYDisparar
        //CREO QUE VA FUERA
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
