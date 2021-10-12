package PROP.robot;

import java.awt.geom.Point2D;
import robocode.*;
import robocode.util.Utils;

/**
 *
 * @author srimp
 */
public class Mrpotato extends AdvancedRobot {
    
    // Atributos de la clase
    private final static double DISTANCA_LIMITE = 200.0;
        
    private double anteriorDirEnemigo;
    private double predX;
    private double predY;
    private int potencia_bala;
    public Mrpotato(){
        this.anteriorDirEnemigo = 0.0;
        this.potencia_bala = 3;
    }
    
    
    @Override
    public void run() {
        // Inicializaciones
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        
        // Giro para triggerear evento onScannedRobot y poder trackear perfecto
        setTurnRadarRight(Double.POSITIVE_INFINITY);
        
        while(true) {
            scan();
            execute();
        }
    }
    
    
    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        analiza(e);
        esquivar(e);
        
        // placeholder 3 energy bullet
        if(getEnergy()>40) {
            potencia_bala = 3;
        }
        else if(getEnergy()>20 && getEnergy()<40) {
            potencia_bala = 2;
        }
        else if (e.getDistance() < 100) {
            potencia_bala = 1;
        }
        circularTarget(e, potencia_bala);
        apuntaYDisparar(e);
        
        // Refresh radar position
        setTurnRadarRight(2.0 * Utils.normalRelativeAngleDegrees(getHeading() + e.getBearing() - getRadarHeading()));
    }
    
    
    @Override
    public void onHitWall(HitWallEvent e) {
        this.setBack(100);
    }
    
    
    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        this.setBack(100);

    }
    
    
    @Override
    public void onBulletHit(BulletHitEvent e) {
        setFire(Rules.MAX_BULLET_POWER);
    }
  
    
    /**
     * Método que analiza la situacion en el campo de batalla respecto al enemigo. Ajustamos la orientacion,
     * velocidad y el movimiento del radar y del cañón
     * @param e Permite obtener información del enemigo
     */
    private void analiza(ScannedRobotEvent e) {
        
        // mirando hacia el otro robot.
        setTurnRight(2.0 * Utils.normalRelativeAngleDegrees(getHeading() + e.getBearing() - getHeading()));
        
        double distanciaActual = e.getDistance();
                
        // si se acerca mas de la distancia limite se tira de cabeza a matarlo
        double distanciaHastaLimit = distanciaActual - DISTANCA_LIMITE;
        
        if (distanciaHastaLimit > 0) {
            this.setAhead(distanciaHastaLimit * 0.1);
        }
        else {
            if (e.getEnergy() < this.getEnergy()) {
                this.setAhead(200);
            }
            else {
                this.setAhead(distanciaHastaLimit * 0.1);
            }
        }
        
    }
    
    
    /**
     * Método que permite al robot esquivar un disparo.
     * @param e 
     */
    private void esquivar(ScannedRobotEvent e) {
        // esto no parece ser necesario y probablemente sea muy complejo.
    }
    
    /**
     * Método usado para identificar el punto hacía donde disparar, según la posición del enemigo, a través de una política de target circular
     * @param e 
     */
    private void circularTarget(ScannedRobotEvent e, int tipoBala) {
        //Calculamos la diferencia de grados entre la anterior posición del enemigo calculada y la nueva. Excepto en el primer calculo dónde
        //la anterior dirección del enemigo está inicializada en 0.0. Además actualizamos la que será la nueva anterior dirección del enemigo
        double actDirEnemigo = e.getHeadingRadians();
        double diferenciaGrados = actDirEnemigo - anteriorDirEnemigo;
        anteriorDirEnemigo = actDirEnemigo;
        
        //Predicción inicial
        //LO QUE VA EN LOS PARENTESIS DE LO DE SIN Y COS SI NO ESTOY PATINANDO ES LA DIRECCION. ASÍ QUE A LO MEJOR ESO HAY QUE GUARDARLO EN UNA VARIABLE Y METERLO EN LA FUNCION DE ANALIZAR
        predX = getX() + e.getDistance()*Math.sin((e.getBearingRadians()+getHeadingRadians()));
        predY = getY() + e.getDistance()*Math.cos((e.getBearingRadians()+getHeadingRadians()));
        //Queremos que la bala de al enemigo, por tanto calculamos iterativamente predicciones hasta que pueda alcanzarla
        //teniendo en cuenta el tiempo transcurrido en cada iteración
        double tiempo = 0;
        while(Point2D.Double.distance(getX(), getY(), predX, predY) > (tiempo*(20-3*tipoBala))){
            //Actualizamos los valores de las coordenadas predecidas anteriormente teniendo en cuenta la dirección y velocidad del enemigo
            predX = predX + e.getVelocity()*Math.sin(actDirEnemigo);
            predY = predY + e.getVelocity()*Math.cos(actDirEnemigo);
            
            actDirEnemigo = actDirEnemigo + diferenciaGrados;
            //LOS BORDES DEL MAPA HAY QUE TENERLOS AQUI EN CUENTA??
            ++tiempo;
        }
    }
    
    
    /**
     * Método que permite al tanque apuntar y disparar a la posición donde predecimos que estará el enemigo.
     * @param e Permite obtener información del enemigo
     */
    private void apuntaYDisparar(ScannedRobotEvent e) {
        // double anguloTanque = Utils.normalAbsoluteAngle(Math.atan2(predX - getX(), predY - getY()));
        // double anguloCanon = Utils.normalRelativeAngle(anguloTanque - getGunHeadingRadians());
        double anguloTanque = Utils.normalRelativeAngle(Math.atan2(predX - getX(), predY - getY()));
        double anguloCanon = Utils.normalRelativeAngle(anguloTanque - getGunHeadingRadians());
        setTurnGunRightRadians(anguloCanon);

        if(getEnergy()>40) {
            setFire(Rules.MAX_BULLET_POWER);
        }
        else if(getEnergy()>20 && getEnergy()<40) {
            setFire(Rules.MAX_BULLET_POWER/2);
        }
        else if (e.getDistance() < 100) {
            setFire(Rules.MIN_BULLET_POWER);
        }

        double anguloradar = Utils.normalRelativeAngle((e.getBearing()+getHeading())-getRadarHeadingRadians());
        setTurnRadarRightRadians(anguloradar);
    }
    
}
