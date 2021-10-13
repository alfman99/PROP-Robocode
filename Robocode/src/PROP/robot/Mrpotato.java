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
    
    private int direccion;
    private int ticksFromLastMovChange;
    private double anteriorDirEnemigo;
    private double predX;
    private double predY;
    private int potencia_bala;
    
    private int margenBorde;
    
    private double prevEnergia;
    
    public Mrpotato(){
        this.anteriorDirEnemigo = 0.0;
        this.potencia_bala = 3;
        this.direccion = 1;
        this.ticksFromLastMovChange = 0;
        this.prevEnergia = Double.MAX_VALUE;
        this.margenBorde = 150;
    }
    
    
    @Override
    public void run() {
        // Inicializaciones
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        
        // Giro para triggerear evento onScannedRobot y poder trackear perfecto
        setTurnRadarRight(Double.POSITIVE_INFINITY);
        
        // Registramos el evento customizado para que se tenga en cuenta en la partida.
        addCustomEvent(this.casiChocoEvent);
        
        while(true) {
            this.ticksFromLastMovChange++;
            scan();
            execute();
        }
    }
    
    
    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        analizaYMueve(e);
        
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
        // System.out.println("me he pegao");
        this.changeDirecction("MURO");
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
     * Cambiar la dirección del movimiento del robot
     * y tener un "threshhold" sobre el que actuar segun
     * los ticks que han pasado desde la ultima vez que 
     * se cambió el movimiento para evitar estar todo el rato
     * cambiando de movimiento y haciendolo inutil.
     * 
     * Se le da mas prioridad a unos eventos que a otros.
     * 
     * @param motivo 
     */
    private void changeDirecction (String motivo) {
                
        if (motivo.equals("MURO")) {
            this.ticksFromLastMovChange = 0;
            direccion *= -1;
        }
        else if (motivo.equals("CASI_CHOCO")) {
            if (this.ticksFromLastMovChange > 10) {
                direccion *= -1;
                this.ticksFromLastMovChange = 0;
            }
        }
        else if (motivo.equals("ENEMIGO_DISPARO")) {
            if (this.ticksFromLastMovChange > 30) {
                direccion *= -1;
            }
            this.ticksFromLastMovChange = 0;
        }
        else if (motivo.equals("PERIODICO")) {
            if (this.ticksFromLastMovChange > 5) {
                direccion *= -1;
            }
        }
        
    }
    
    /**
     * Crear nuevo evento en el que devuelve true si nuestro robot sobrepasa
     * el borde del ring para cambiar la dirección en el handler del evento
     * antes de que choque asi evitando perder puntos por chocar contra el borde.
     */
    Condition casiChocoEvent = new Condition("CASI_CHOCO") {
        @Override
        public boolean test() {
            return (
                !(
                    getX() > margenBorde &&
                    getX() < (getBattleFieldWidth() - margenBorde) &&
                    getY() > margenBorde &&
                    getY() < (getBattleFieldHeight() - margenBorde)
                )
             );
        }
    };

    
    @Override
    public void onCustomEvent(CustomEvent e) {
        if (e.getCondition().getName().equals("CASI_CHOCO"))
        {
            this.changeDirecction("CASI_CHOCO");
        }
    }
    
    /**
     * Normaliza el angulo para que esté comprendido entre 0 y 360º
     * @param angle
     * @return 
     */
    private double normalizeBearing(double angle) {
        return angle % 360;
    }
  
    
    /**
     * Método que analiza la situacion en el campo de batalla respecto al enemigo. Ajustamos la orientacion,
     * velocidad y el movimiento del radar y del cañón
     * @param e Permite obtener información del enemigo
     */
    private void analizaYMueve(ScannedRobotEvent e) {
        
        // posicionar el robot en perpendicular a la recta que forman los dos tanques
        setTurnRight(e.getBearing() + 90);
        
        double distanciaActual = e.getDistance();
                
        // si se acerca mas de la distancia limite se tira de cabeza a matarlo
        double distanciaHastaLimit = distanciaActual - DISTANCA_LIMITE;
        
        if (distanciaHastaLimit > 0) {
            setTurnRight(normalizeBearing(e.getBearing() + 90 - (15 * direccion)));
        }
        else {
            if (e.getEnergy() < this.getEnergy()) {
                setTurnRadarRight(2.0 * Utils.normalRelativeAngleDegrees(getHeading() + e.getBearing() - getRadarHeading()));
            }
            else {
                setTurnRight(normalizeBearing(e.getBearing() + 90 + (15 * direccion)));
            }
        }
        
        if (e.getEnergy() < this.prevEnergia) {
            this.changeDirecction("ENEMIGO_DISPARO");
        }
        
        this.prevEnergia = e.getEnergy();
        
        if (getTime() % 20 == 0) {
            this.changeDirecction("PERIODICO");
        }
        
        this.setAhead(200 * this.direccion);
        
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
