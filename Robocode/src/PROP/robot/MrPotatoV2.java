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
    protected final static double DISTANCE_MAX_POWER = 250;
    
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
     * Inicializa el robot para que sea operativo
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
     * Bucle principal del robot
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
     * Incrementa diferentes tickers
     */
    protected void incTickers () {
        this.ticksFromLastMovChange++;
    }
    
    /**
     * Normaliza el angulo entre 0 y 360º
     * @param angle Angulo no normalizado
     * @return Angulo normalizado
     */
    protected double normalizeBearing(double angle) {
        return angle % 360;
    }
    
    /**
     * Handler para diferentes posibles cambios de dirección
     * de nuestro robot.
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
     * Lógica principal del movimiento de nuestro robot
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
     * Lógica para elegir la potencia de la bala
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
     * Calcula la distancia que hay entre nuestro robot y la posición que se
     * ha predecido con this.predX y this.predY
     * @return La distancia que hay entre nuestro robot y la posición que se
     * ha predecido con this.predX y this.predY
     */
    protected double distanceFromOurPostoPred () {
        return Point2D.Double.distance(getX(), getY(), this.predX, this.predY);
    }
    
    
    /**
     * Calcula a que distancia estará una bala de potencia this.bulletPower en
     * un tiempo determinado time.
     * @param time Tiempo usado para el calculo
     * @return Devuelve la distancia calculada
     */
    protected double calcDistanceToBullet (double time) {
        return time * (20 - 3 * this.bulletPower);
    }
    
    
    /**
     * Ajustar los atributos de posición en donde se quiere disparar con un 
     * modelo de predicción basado en el circular targeting.
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
     * En función de los atributos this.predX y this.predY calculamos el angulo
     * en el que el cañon del tanque debe estar a la hora de disparar y disparamos.
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
     * Evento principal de donde se toman todas las decisiones basado en 
     * la información que recibimos del enemigo escaneado.
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
     * Creamos un evento personalizado para determinar si nuestro robot está
     * en una zona peligrosa o no en función del la distancia a los muros que le 
     * rodean.
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
     * Handler donde se procesarán los eventos personalizados creados por nosotros.
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
     * Handler que determina que hacer si nos chocamos contra un muro
     * @param event Permite obtener información del enemigo utilizada para tomar decisiones
     */
    @Override
    public void onHitWall(HitWallEvent event) {
        this.changeDirection("MURO");
    }
    
}
