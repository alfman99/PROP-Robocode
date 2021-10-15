/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PROP.robot;

import java.awt.geom.Point2D;
import robocode.AdvancedRobot;
import robocode.Condition;
import robocode.CustomEvent;
import robocode.HitWallEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

/**
 *
 * @author srimp
 */
public class MrPotatoV2 extends AdvancedRobot {
    
    /**
     * Originalmente todos los atributos / metodos marcados como protected eran
     * private, pero a la hora de generar el javadoc no se mostraban
     * ya que no tiene sentido documentar metodos o atributos que no van a ser
     * utilizados por otras clases.
     */
    
    // Movement constant
    protected final static double DISTANCE_TO_ROBOT = 200.0;
    protected final static int DISTANCE_TO_BORDER = 150;
    protected final static double DISTANCE_MAX_POWER = 500;
    
    // Movement attributes
    protected int ticksFromLastMovChange;
    protected int headingDirection;
    
    // Shooting attributes
    protected double lastEnemyHeading;
    protected double predX;
    protected double predY;
    protected double bulletPower;
    
    // Enemy tracking
    protected double prevEnergia;
    
    /**
     *
     */
    public MrPotatoV2(){
        this.ticksFromLastMovChange = 0;
        this.headingDirection = 1;
        
        this.lastEnemyHeading = 0.0;
        this.predX = 0.0;
        this.predY = 0.0;
        this.bulletPower = Rules.MAX_BULLET_POWER;
        
        this.prevEnergia = Double.MAX_VALUE;
    }
    
    /**
     * Función usada para inicializar el robot. Orientamos el radar y el cañón.
     * Rotamos el radar para comenzar a seguir al enemigo. 
     * Registramos un evento customizado para su uso.
     */
    protected void init () {
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        
        // Giro para triggerear evento onScannedRobot y poder trackear perfecto
        setTurnRadarRight(Double.POSITIVE_INFINITY);
        
        // Registramos el evento customizado para que se tenga en cuenta en la partida.
        addCustomEvent(this.almostHitWall);
    }
    
    /**
     * Punto de entrada donde se inicializa el robot y se ejecuta el bucle principal de la lógica del robot.
     */
    @Override
    public void run() {
        this.init();
        while(true) {
            this.incTickers();
            scan();
            execute();
        }
    }
    
    /**
     * Función para incrementar cada uno de los ticks que estamos siguiendo, se ejecuta una vez por cada tick del juego.
     */
    protected void incTickers () {
        this.ticksFromLastMovChange++;
    }
    
    /**
     * Función para normalizar el ángulo angle entre 0 y 360º
     * @param angle Angulo no normalizado
     * @return Angulo normalizado
     */
    protected double normalizeBearing(double angle) {
        return angle % 360;
    }
    
    /**
     * Handler que permite cambiar la dirección de nuestro robot en función de diferentes eventos que sucedan en la partida (motivo). 
     * Cambiaremos la dirección en caso de que nos encontremos con un muro (MURO),en caso de que casi chocamos (CASI_CHOCO),
     * en caso de que nos dispare un enemigo (ENEMIGO_DISPARO) y para movernos haciendo los strafes(zig-zags) (PERIODICO).
     * @param motivo Motivo por el que se quiere cambiar de dirección
     */
    protected void changeDirection (String motivo) {
        switch (motivo) {
            case "MURO": {
                this.headingDirection *= -1;
                this.ticksFromLastMovChange = 0;
                break;
            }
            case "CASI_CHOCO": {
                if (this.ticksFromLastMovChange > 5) {
                    this.headingDirection *= -1;
                    this.ticksFromLastMovChange = 0;
                }
                break;
            }
            case "ENEMIGO_DISPARO": {
                if (this.ticksFromLastMovChange > 30) {
                    this.headingDirection *= -1;
                }
                this.ticksFromLastMovChange = 0;
                break;
            }
            case "PERIODICO": {
                if (this.ticksFromLastMovChange > 5) {
                    this.headingDirection *= -1;
                }
                break;
            }
            default: {
                System.out.println(motivo + " unhandled.");
            }
        }
    }
    
    
    /**
     * Función que otorga al robot la lógica principal de movimiento.
     * MrPotatoV2 se posiciona de forma perpendicular al enemigo y se mantiene a una distancia determinada moviéndose en zig-zag reculando o avanzando
     * manteniendo dicha distancia. Si la distancia no se puede mantener y la energía de MrPotatoV2 es superior a la energía de
     * su contrincante, MrPotatoV2 avanzará hacia adelante intentando chocarle hasta la muerte.
     * @param event Permite obtener información del enemigo utilizada para tomar decisiones
     */
    protected void analizaYMueve(ScannedRobotEvent event) {
        
        // Reiniciamos la posicion a perpendicular al enemigo
        setTurnRight(event.getBearing() + 90);
        
        double distanceToLimit = event.getDistance() - DISTANCE_TO_ROBOT;
        
        // Queremos acercarnos al limite
        if (distanceToLimit > 0) {
            double angle = event.getBearing() + 90 - ( 15 * headingDirection );
            double normalizedAngle = this.normalizeBearing(angle);
            setTurnRight(normalizedAngle);
        }
        else {
            if (event.getEnergy() < this.getEnergy()) {
                setTurnRadarRight(2.0 * Utils.normalRelativeAngleDegrees(getHeading() + event.getBearing() - getRadarHeading()));
            }
            else {
                double angle = event.getBearing() + 90 + ( 15 * headingDirection );
                double normalizedAngle = this.normalizeBearing(angle);
                setTurnRight(normalizedAngle);
            }
        }
        
        if (event.getEnergy() < this.prevEnergia) {
            this.changeDirection("ENEMIGO_DISPARO");
        }
        
        if (getTime() % 20 == 0) {
            this.changeDirection("PERIODICO");
        }
        
        this.prevEnergia = event.getEnergy();
        this.setAhead(200 * this.headingDirection);
        
    }
    
    
    /**
     * Función que decide con qué potencia vamos a disparar la próxima vez que disparemos.
     * @param event Permite obtener información del enemigo utilizada para tomar decisiones
     */
    protected void selectBulletPower(ScannedRobotEvent event) {
        if (event.getDistance() > DISTANCE_MAX_POWER) {
            this.bulletPower = Rules.MIN_BULLET_POWER;
        }
        else if (getEnergy() >= 0 && getEnergy() <= 20) {
            this.bulletPower = Rules.MAX_BULLET_POWER / 1.5f;
        }
        else {
            this.bulletPower = Rules.MAX_BULLET_POWER;
        }
        
    }
    
    
    /**
     * Función que devuelve la distancia euclídea entre nuestro robot y el punto que hemos predecido (predX, predY)
     * @return La distancia que hay entre nuestro robot y la posición que se
     * ha predecido con this.predX y this.predY
     */
    protected double distanceFromOurPostoPred () {
        return Point2D.Double.distance(getX(), getY(), this.predX, this.predY);
    }
    
    
    /**
     * Función que calcula a qué distancia estará una bala con la potencia establecida en ese momento en un tiempo determinado (time).
     * @param time Tiempo usado para el calculo
     * @return Devuelve la distancia calculada
     */
    protected double calcDistanceToBullet (double time) {
        return time * (20 - 3 * this.bulletPower);
    }
    
    
    /**
     * Función que permite calcular y ajustar los atributos de posición donde se quiere disparar. 
     * El modelo de predicción está basado en el circular targeting el cual asume que el robot enemigo va a
     * continuar moviéndose siguiendo una trayectoria determinada a una velocidad determinada.  
     * @param event Permite obtener información del enemigo utilizada para tomar decisiones
     */
    protected void calculateCircularTarget (ScannedRobotEvent event) {
        double enemyHeading = event.getHeadingRadians();
        double headingDiffRad = enemyHeading - this.lastEnemyHeading;
        
        double totalSum = event.getBearingRadians() + getHeadingRadians();
        this.predX = getX() + event.getDistance() * Math.sin(totalSum);
        this.predY = getY() + event.getDistance() * Math.cos(totalSum);
        this.lastEnemyHeading = enemyHeading;
        
        double time = 0.0;
        while (this.distanceFromOurPostoPred() > this.calcDistanceToBullet(time)) {
            this.predX = this.predX + event.getVelocity() * Math.sin(enemyHeading);
            this.predY = this.predY + event.getVelocity() * Math.cos(enemyHeading);
        
            enemyHeading += headingDiffRad;
            
            time++;
        }
    }
    
    
    /**
     * Función encargada de calcular el ángulo con respecto a la posición dada previamente en la función calculateCircularTarget(),
     * de ajustarlo y de disparar.
     * @param event Permite obtener información del enemigo utilizada para tomar decisiones
     */
    protected void aimAndShoot(ScannedRobotEvent event) {
                
        // Angulo que forman el tanque y su target predecido en RADIANES
        double angleTankObjRadians = Math.atan2(predX - getX(), predY - getY());
        
        // Normaliza el angulo a en RADIANES
        double anguloTanque = this.normalizeBearing(angleTankObjRadians);   
     
        // Calcular angulo en RADIANES del cañon relativo al angulo del tanque
        double anguloCanon = Utils.normalRelativeAngle(anguloTanque - getGunHeadingRadians());
        
        
        setTurnGunRightRadians(anguloCanon);
        
        setFire(this.bulletPower);
    }
    
    
    /**
     * Función que es llamada siempre que un robot enemigo es escaneado. 
       La lógica principal de nuestro robot proviene de este handler, ya que MrPotatoV2 toma decisiones según 
       * lo que se procese al escanear el estado de la batalla. Llamamos a los siguientes métodos en orden:
        1. analizaYmueve
        2. selectBulletPower
        3. calculateCircularTarget
        4. aimAndShoot
       Por último refrescamos hacia donde está mirando el radar en función de la nueva posición del robot escaneado.

     * @param event Permite obtener información del enemigo utilizada para tomar decisiones
     */
    @Override
    public void onScannedRobot (ScannedRobotEvent event) {
        this.analizaYMueve(event);
        this.selectBulletPower(event);
        this.calculateCircularTarget(event);
        this.aimAndShoot(event);
        
        // Refresh radar position
        setTurnRadarRight(2.0 * Utils.normalRelativeAngleDegrees(getHeading() + event.getBearing() - getRadarHeading()));
    }
    
    /**
     * Nueva condición creada para registrar un evento personalizado, en el que devolvemos true o false. 
     * Según si la posición del robot está demasiado cerca del borde o no.
     */
    Condition almostHitWall = new Condition("CASI_CHOCO") {
        @Override
        public boolean test() {
            return !(
                    getX() > DISTANCE_TO_BORDER &&
                    getX() < (getBattleFieldWidth() - DISTANCE_TO_BORDER) &&
                    getY() > DISTANCE_TO_BORDER &&
                    getY() < (getBattleFieldHeight() - DISTANCE_TO_BORDER)
            );
        }
    };
    
    
    /**
     * Función que permite procesar los eventos personalizados.
     * @param event Permite obtener información del enemigo utilizada para tomar decisiones
     */
    @Override
    public void onCustomEvent(CustomEvent event) {
        // Esto en verdad no haría falta ya que solo tenemos un custom event.
        if (event.getCondition().getName().equals("CASI_CHOCO")) {
            this.changeDirection("CASI_CHOCO");
        }
    }
    
    
    /**
     * Función que determina qué hacer si nos chocamos con un muro.
     * @param event Permite obtener información del enemigo utilizada para tomar decisiones
     */
    @Override
    public void onHitWall(HitWallEvent event) {
        this.changeDirection("MURO");
    }
    
}
