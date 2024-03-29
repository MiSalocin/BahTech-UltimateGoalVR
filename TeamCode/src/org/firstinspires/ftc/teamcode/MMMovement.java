package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.bosch.JustLoggingAccelerationIntegrator;
import com.qualcomm.robotcore.hardware.*;

import org.firstinspires.ftc.robotcore.external.navigation.*;

public class MMMovement {
    // Abstract Variables
    private final double[] force = new double[4];
    private final double[] lastForce = new double[4];
    private final int smoother = 25;

    private double angle;
    private double internal = 1;
    private double external = 1;
    private int armGoal = 0;
    private int armPos = 0;
    private int shooter;

    // Setting up motors variables
    private DcMotor FR;
    private DcMotor FL;
    private DcMotor BR;
    private DcMotor BL;
    private DcMotor shooterMotor;
    private DcMotor armMotor;
    private Servo shooterServo;
    private Servo clawServo;


    // IMU variables
    private BNO055IMU imu;
    private final BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();

    // Program used to define the hardware variables
    public void defHardware(HardwareMap local) {

        // Define IMU
        imu = local.get(BNO055IMU.class, "imu");
        parameters.angleUnit = BNO055IMU.AngleUnit.DEGREES;
        parameters.calibrationDataFile = "BNO055IMUCalibration.json";
        parameters.loggingEnabled = true;
        parameters.loggingTag = "IMU";
        parameters.accelerationIntegrationAlgorithm = new JustLoggingAccelerationIntegrator();
        imu.initialize(parameters);


        // Motors used in the movement
        FL = local.dcMotor.get("front_left_motor");
        FR = local.dcMotor.get("front_right_motor");
        BL = local.dcMotor.get("back_left_motor");
        BR = local.dcMotor.get("back_right_motor");
        clawServo = local.servo.get("arm_servo");
        armMotor = local.dcMotor.get("arm_motor");
        shooterServo = local.servo.get("shooter_trig_servo");
        shooterMotor = local.dcMotor.get("shooter_motor");
        DcMotor intake = local.dcMotor.get("intake_motor");

        shooterMotor.setPower(1);
        intake.setPower(1);
        armMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        armMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        BL.setDirection(DcMotorSimple.Direction.REVERSE);
        FL.setDirection(DcMotorSimple.Direction.REVERSE);
    }

    // Program used to move the robot using the arena
    public void moveByArena(double leftY, double leftX, double rightX, boolean slower, boolean faster) {

        // Create a variable using the IMU
        Orientation angles = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);
        double angle = angles.firstAngle;

        // Will see the IMU value and change the "intern" and "extern" variables
        if (0 < angle && angle <= 90) {
            internal = -(angle / 45 - 1);
            external = 1;
        } else if (90 < angle && angle <= 180) {
            internal = -1;
            external = -((angle - 90) / 45 - 1);
        } else if (-90 < angle && angle <= 0) {
            internal = 1;
            external = angle / 45 + 1;
        } else if (-180 < angle && angle <= 90) {
            internal = (angle + 90) / 45 + 1;
            external = -1;
        }

        // Will start the normal "MoveByRobot" program, but using the Intern and Extern Multipliers
        moveByRobot(-leftY, -leftX, -rightX, slower, faster);
    }

    // Program used to move the robot by himself
    // Define the same commands given in the MoveByArena
    public void moveByRobot(double leftY, double leftX, double rightX, boolean slower, boolean faster) {
        final double smoother = 0.1;

        // Add to the vector the forces of the gamepad multiplying this with the angle defined
        // in the "MoveByArena" program.
        force[0] = leftY * internal + leftX * external + rightX;
        force[1] = leftY * external - leftX * internal + rightX;
        force[2] = leftY * external - leftX * internal - rightX;
        force[3] = leftY * internal + leftX * external - rightX;

        // See if the difference of the last force and the current one is bigger than 0.1,
        // if it is, it will change gradually to not damage the motors
        if (Math.abs(lastForce[0] - force[0]) > smoother) {
            if (lastForce[0] > force[0]) force[0] = lastForce[0] - smoother;
            else force[0] = lastForce[0] + smoother;
        }
        if (Math.abs(lastForce[1] - force[1]) > smoother) {
            if (lastForce[1] > force[1]) force[1] = lastForce[1] - smoother;
            else force[1] = lastForce[1] + smoother;
        }
        if (Math.abs(lastForce[2] - force[2]) > smoother) {
            if (lastForce[2] > force[2]) force[2] = lastForce[2] - smoother;
            else force[2] = lastForce[2] + smoother;
        }
        if (Math.abs(lastForce[3] - force[3]) > smoother) {
            if (lastForce[3] > force[3]) force[3] = lastForce[3] - smoother;
            else force[3] = lastForce[3] + smoother;
        }

        // Save the used force in variables to get the difference
        lastForce[0] = force[0];
        lastForce[1] = force[1];
        lastForce[2] = force[2];
        lastForce[3] = force[3];

        // If the right bumper is pressed, the robot will move slower
        if (slower) {
            FR.setPower(force[0] / 4);
            BR.setPower(force[1] / 4);
            FL.setPower(force[2] / 4);
            BL.setPower(force[3] / 4);
        }
        // If the right bumper is pressed, the robot will move faster
        else if (faster) {
            FR.setPower(force[0]);
            BR.setPower(force[1]);
            FL.setPower(force[2]);
            BL.setPower(force[3]);
        }
        // If neither bumper is pressed, the robot will move half the speed
        else {
            FR.setPower(force[0] / 2);
            BR.setPower(force[1] / 2);
            FL.setPower(force[2] / 2);
            BL.setPower(force[3] / 2);
        }
    }

    // Program used to turn the robot to one side.
    // define the angle in degrees, if it shout turn left or right and the force
    public void turn(double force, boolean right, double targetAngle) {
        double smoother = 25;
        double currentAngle = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES).firstAngle;
        if (right) {
            angle = - targetAngle + currentAngle;
            if (angle < -180) {
                angle += 360;
            }
            if (angle - currentAngle > 180) {
                currentAngle += 360;
            }
            while (angle <= currentAngle) {
                currentAngle = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES).firstAngle;
                if (angle - currentAngle > 180)
                    currentAngle += 360;
                if (force < ((angle - currentAngle) / smoother)) {
                    FR.setPower(-force);
                    BR.setPower(-force);
                    FL.setPower( force);
                    BL.setPower( force);
                } else {
                    FR.setPower(-(angle - currentAngle) / smoother);
                    BR.setPower(-(angle - currentAngle) / smoother);
                    FL.setPower( (angle - currentAngle) / smoother);
                    BL.setPower( (angle - currentAngle) / smoother);
                }
            }
        } else {
            angle = targetAngle + currentAngle;
            if (angle > 180) {
                angle -= 360;
            }
            if (currentAngle - angle > 180)
                currentAngle -= 360;
            while (angle >= currentAngle) {
                currentAngle = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES).firstAngle;
                if (currentAngle - angle > 180)
                    currentAngle -= 360;
                if (force < ((currentAngle - angle) / smoother)) {
                    FR.setPower(force);
                    BR.setPower(force);
                    FL.setPower(-force);
                    BL.setPower(-force);
                } else {
                    FR.setPower((currentAngle - angle) / smoother);
                    BR.setPower((currentAngle - angle) / smoother);
                    FL.setPower(-(currentAngle - angle) / smoother);
                    BL.setPower(-(currentAngle - angle) / smoother);
                }
            }
        }
        FR.setPower(0);
        BR.setPower(0);
        FL.setPower(0);
        BL.setPower(0);
    }

    // Program used to turn the robot 180 degrees.
    // define the motors force
    public void inverter(double force) {
        // Calculate the objective degree
        double smoother = 25;
        double currentAngle = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES).firstAngle;
        if (currentAngle < 0.4 && currentAngle > -0.4) {
            angle = 179.7;
        } else if (currentAngle > 0) {
            angle = -180 + currentAngle;
        } else {
            angle = 180 + currentAngle;
        }

        // Choose if it will turn right or left
        if (angle >= currentAngle) {
            while (angle >= currentAngle) {
                currentAngle = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES).firstAngle;
                if (force < ((angle - currentAngle) / smoother)) {
                    FR.setPower(-force);
                    BR.setPower(-force);
                    FL.setPower( force);
                    BL.setPower( force);
                } else {
                    FR.setPower(-(angle - currentAngle) / smoother);
                    BR.setPower(-(angle - currentAngle) / smoother);
                    FL.setPower( (angle - currentAngle) / smoother);
                    BL.setPower( (angle - currentAngle) / smoother);
                }
            }
        } else {
            while (angle <= currentAngle) {
                currentAngle = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES).firstAngle;
                if (force < ((currentAngle - angle) / smoother)) {
                    FR.setPower( force);
                    BR.setPower( force);
                    FL.setPower(-force);
                    BL.setPower(-force);
                } else {
                    FR.setPower(( currentAngle - angle) / smoother);
                    BR.setPower(( currentAngle - angle) / smoother);
                    FL.setPower(-(currentAngle - angle) / smoother);
                    BL.setPower(-(currentAngle - angle) / smoother);
                }
            }
        }
        FR.setPower(0);
        BR.setPower(0);
        FL.setPower(0);
        BL.setPower(0);
    }

    // Program used to shoot the rings to the high goals/power shots
    public void shoot(boolean trigger, double force) throws InterruptedException {
        shooterMotor.setPower(force);
        if (trigger) {
            shooterServo.setPosition(1);
            wait(500);
        } else {
            shooterServo.setPosition(0);
        }
    }

    // Program used to move the robot claw
    public void claw(boolean up, boolean down) {

        //change this constant to make the claw move smoother/slower
            final int smoother = 15;

        if (Math.abs(armPos - armGoal) > smoother) {
            if (armPos > armGoal) armGoal = armPos - smoother;
            else armGoal = armPos + smoother;
        }
        lastForce[0] = force[0];
    }


// Getters and setters
    public double getInternal() { return internal; }
    public double getExternal() { return external; }
    public double getAngles() { return imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES).firstAngle;}
    public double getFlForce(){ return FL.getPower(); }
    public double getFrForce(){ return FR.getPower(); }
    public double getBlForce(){ return BL.getPower(); }
    public double getBrForce(){ return BR.getPower(); }
}