package org.firstinspires.ftc.teamcode;

//Import the general libraries to use in the program

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name = "MMCore", group = "BahTech")
public class MMCore extends LinearOpMode {

    // Map the hardware to autonomous and TeleOp
    final MMMovement move = new MMMovement();

    @Override
    public void runOpMode() throws InterruptedException {

        // Create the Hardware Map and start IMU
        move.defHardware(hardwareMap);

        // Run once you press PLAY
        waitForStart();

        // Run repeatedly after you press play
        while(opModeIsActive()){

            // Create variables for the control
            double leftX = gamepad1.left_stick_x;
            double leftY = gamepad1.left_stick_y;
            double rightX = gamepad1.right_stick_x;
            boolean lb = gamepad1.left_bumper;
            boolean rb = gamepad1.right_bumper;

            if (gamepad1.y) move.inverter(1);
            if (gamepad1.x) move.inverter(1);
            move.shoot(gamepad1.right_trigger > 50, 1);
            move.shoot(gamepad1.left_trigger > 50, 0.8);
            move.moveByArena(leftY, leftX, rightX, lb, rb);

            telemetry.addData("Angles", String.format("%.2f", move.getAngles()));
            telemetry.addData("Front Left Motor", String.format("%.2f", move.getFlForce()));
            telemetry.addData("Front Right Motor", String.format("%.2f", move.getFrForce()));
            telemetry.addData("Back Left Motor", String.format("%.2f", move.getBlForce()));
            telemetry.addData("Back Right Motor", String.format("%.2f", move.getBrForce()));
            telemetry.update();
        }
    }
}