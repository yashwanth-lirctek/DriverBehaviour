package com.lirctek.driverbehaviour

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.media.MediaPlayer
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.lirctek.driverbehaviour.data.MainAdapter
import com.lirctek.driverbehaviour.data.ObjectBox
import com.lirctek.driverbehaviour.data.Prefs
import com.lirctek.driverbehaviour.data.SpeedData
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*


class MainActivity : AppCompatActivity(), SensorEventListener {

    lateinit var mRemoveButton: FloatingActionButton
    lateinit var mAddButton: FloatingActionButton
    lateinit var mSpeedLimit: ExtendedFloatingActionButton
    lateinit var mRecyclerView: RecyclerView
    lateinit var mSpeed: TextView
    lateinit var mClear: ImageView
    lateinit var mNoData: TextView

    lateinit var mSensorManager: SensorManager

    var speedLimit = 40
    var currentSpeed = 0.0f
    var maxSpeed = 0
    var mph = 0

    var xAccelerometer = 0f
    var yAccelerometer = 0f
    var zAccelerometer = 0f

    var xPreviousAcc = 0f
    var yPreviousAcc = 0f
    var zPreviousAcc = 0f

    var xAccCalibrated = 0f
    var yAccCalibrated = 0f
    var zAccCalibrated = 0f

    var timestamp = 0L

    var initState = true
    var mInitialized = false
    var writeCheck = false

    var gyro = FloatArray(3)
    var gyroMatrix = FloatArray(9)
    var gyroOrientation = FloatArray(3)
    var rotationMatrix = FloatArray(9)
    var accMagOrientation = FloatArray(3)
    var fusedOrientation = FloatArray(3)
    var magnet = FloatArray(3)
    var accel = FloatArray(3)

    var pitchOut = 0f
    var rollOut = 0f
    var yawOut = 0f

    var count = 1

    //counter for accelerometer reading
    var overX = 0
    var overY = 0

    var mMagneticField: FloatArray? = null
    var mGravity: FloatArray? = null

    val EPSILON = 0.000000001f
    val TIME_CONSTANT = 10L
    private val NS2S = 1.0f / 1000000000.0f

    var getPitch = 0f
    var getRoll = 0f
    var getYaw = 0f

    var getPitchQ = 0f
    var getRollQ = 0f
    var getYawQ = 0f

    var newPitchOut = 0f
    var newRollOut = 0f
    var newYawOut = 0f

    var newPitchOutQ = 0f
    var newRollOutQ = 0f
    var newYawOutQ = 0f

    var mPitch = 0f
    var mRoll = 0f
    var mYaw = 0f

    var getFinalOverYaw = 0
    var getFinalOverPitch = 0
    var getFinalOverX = 0
    var getFinalOverY = 0

    var finalOverYaw = 0f
    var finalOverPitch = 0f

    var overYaw = 0
    var overPitch = 0

    //counter for quaternion
    var overYawQ = 0
    var overPitchQ = 0

    var yAccChange = false
    var xAccChange = false

    var tBreakStart: Long = 0
    var tBreakEnd = 0L
    var suddenBreaksCount = 0
    var tempSpeed = 0f
    var turns = 0
    var suddenAcceleration = 0
    var isBrakesApplied = false

    var limitExceedCount = 0
    var flag = 0

    private val fuseTimer: Timer = Timer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ObjectBox.init(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        startService(Intent(this, LocationTracker::class.java))

        mRemoveButton = findViewById(R.id.mRemoveButton)
        mAddButton = findViewById(R.id.mAddButton)
        mSpeedLimit = findViewById(R.id.mSpeedLimit)
        mRecyclerView = findViewById<RecyclerView>(R.id.mRecyclerView)
        mSpeed = findViewById(R.id.mSpeed)
        mClear = findViewById(R.id.mClear)
        mNoData = findViewById(R.id.mNoData)

        tBreakStart = System.currentTimeMillis()
        suddenBreaksCount = 0
        suddenAcceleration = 0

        initData()

        mSensorManager = this.getSystemService(SENSOR_SERVICE) as SensorManager

        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST)
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST)
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST)

        fuseTimer.scheduleAtFixedRate(calculateFusedOrientationTask(), 2000L, TIME_CONSTANT)
        // analysing behavior every 2 sec
        fuseTimer.scheduleAtFixedRate(BehaviorAnalysis(), 1000L, 2000L)
        fuseTimer.scheduleAtFixedRate(ResetSensorValues(), 1000L, 30000L)

        mClear.setOnClickListener {
            Toast.makeText(this, "Data Cleared Successfully", Toast.LENGTH_SHORT).show()
            SpeedData.clearAllData()
            initData()
        }

        speedLimitData()

        mAddButton.setOnClickListener {

            Prefs.getAppPref().speedLimit += 5
            speedLimitData()

        }

        mRemoveButton.setOnClickListener {

            Prefs.getAppPref().speedLimit -= 5
            speedLimitData()

        }

    }

    private fun speedLimitData() {

        mSpeedLimit.text = "Speed Limit : "+Prefs.getAppPref().speedLimit+" MPH"
        speedLimit = Prefs.getAppPref().speedLimit
    }

    override fun onSensorChanged(event: SensorEvent) {
        updateValues()
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                mGravity = event.values
                xAccelerometer = event.values[0]
                yAccelerometer = event.values[1]
                zAccelerometer = event.values[2]
                calibrateAccelerometer()
                calculateAccMagOrientation()
            }
            Sensor.TYPE_GYROSCOPE ->{
                gyroFunction(event)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                mMagneticField = event.values
            }
        }
        displayQuaternion()
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    private fun updateValues() {
        if (newPitchOut != 0f && newPitchOutQ != 0f && newYawOut != 0f && newYawOutQ != 0f && xAccCalibrated != 0f && yAccCalibrated != 0f) {
            writeCheck = false
            xAccChange = false
            yAccChange = false
            count += 1
            if (count == 2250) {
                count = 1
            }
            if (newYawOut > .30 || newYawOut < -.30) {
                overYaw += 1
                writeCheck = true
            }
            if (newPitchOut > .12 || newPitchOut < -.12) {
                overPitch += 1
                writeCheck = true
            }
            if (newYawOutQ > .30 || newYawOutQ < -.30) {
                overYawQ += 1
                writeCheck = true
            }
            if (newPitchOutQ > .12 || newPitchOutQ < -.12) {
                overPitchQ += 1
                writeCheck = true
            }
            if (xAccCalibrated > 3 || xAccCalibrated < -3) {
                overX = overX + 1
                writeCheck = true
                xAccChange = true
            }
            if (yAccCalibrated > 2.5 || yAccCalibrated < -2.5) {
                overY = overY + 1
                writeCheck = true
                yAccChange = true
            }

            // computing final values for pitch and yaw counters
            if (overPitch != 0 || overPitchQ != 0) {
                finalOverPitch = ((overPitch + 0.3 * overPitchQ).toFloat())
            }
            if (overYaw != 0 || overYawQ != 0) {
                finalOverYaw = ((overYaw + 0.4 * overYawQ).toFloat())
            }

            /*
            Here, one counter on any sensor doesn't reflect the crossing of threshold for 1 time,
            it just gives the total number of times the data was recorded during "1 crossing"
            For one time the user makes a rash turn, counter was reach upto 10 for that one single incident
            */

            // only saving if there is change in the counters
            if (writeCheck) {

//                //Creating a shared preference
//                val sharedPreferences: SharedPreferences =
//                    this@MapsActivity.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
//
//                //Creating editor to store values to shared preferences
//                val editor = sharedPreferences.edit()
//
//                //Adding values to editor
//                editor.putInt("overPitch", finalOverPitch)
//                editor.putInt("overYaw", finalOverYaw)
//                editor.putInt("overX", overX)
//                editor.putInt("overY", overY)
//
//                //Saving values to editor
//                editor.commit()
//                Log.i("MapsActivity", "finalOverPitch : $finalOverPitch")
            }
        }
    }

    private fun displayQuaternion() {
        val R = FloatArray(9)
        val I = FloatArray(9)
        if (mMagneticField != null && mGravity != null) {
            val success = SensorManager.getRotationMatrix(R, I, mGravity, mMagneticField)
            if (success) {
                val mOrientation = FloatArray(3)
                val mQuaternion = FloatArray(4)
                SensorManager.getOrientation(R, mOrientation)
                SensorManager.getQuaternionFromVector(mQuaternion, mOrientation)
                mYaw = mQuaternion[1] // orientation contains: azimuth(yaw), pitch and Roll
                mPitch = mQuaternion[2]
                mRoll = mQuaternion[3]
                newPitchOutQ = getPitchQ - mPitch
                newRollOutQ = getRollQ - mRoll
                newYawOutQ = getYawQ - mYaw
                getPitchQ = mPitch
                getRollQ = mRoll
                getYawQ = mYaw
            }
        }
    }

    fun gyroFunction(event: SensorEvent) {

        // don't start until first accelerometer/magnetometer orientation has been acquired
        if (accMagOrientation.isEmpty()) return

        // initialisation of the gyroscope based rotation matrix
        if (initState) {
            var initMatrix: FloatArray = FloatArray(9)
            initMatrix = getRotationMatrixFromOrientation(accMagOrientation)
            val test = FloatArray(3)
            SensorManager.getOrientation(initMatrix, test)
            gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix)
            initState = false
        }

        // copy the new gyro values into the gyro array
        // convert the raw gyro data into a rotation vector
        val deltaVector = FloatArray(4)
        if (timestamp != 0L) {
            val dT: Float = (event.timestamp - timestamp) * NS2S
            System.arraycopy(event.values, 0, gyro, 0, 3)
            getRotationVectorFromGyro(gyro, deltaVector, dT / 2.0f)
        }

        // measurement done, save current time for next interval
        timestamp = event.timestamp

        // convert rotation vector into rotation matrix
        val deltaMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector)

        // apply the new rotation interval on the gyroscope based rotation matrix
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix)

        // get the gyroscope based orientation from the rotation matrix
        SensorManager.getOrientation(gyroMatrix, gyroOrientation)
    }

    private fun calibrateAccelerometer() {
        if (!mInitialized) {
            xPreviousAcc = xAccelerometer
            yPreviousAcc = yAccelerometer
            zPreviousAcc = zAccelerometer
            mInitialized = true
        } else {
            xAccCalibrated = xPreviousAcc - xAccelerometer
            yAccCalibrated = yPreviousAcc - yAccelerometer
            zAccCalibrated = zPreviousAcc - zAccelerometer
            xPreviousAcc = xAccelerometer
            yPreviousAcc = yAccelerometer
            zPreviousAcc = zAccelerometer
        }
    }

    fun calculateAccMagOrientation() {
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
            SensorManager.getOrientation(rotationMatrix, accMagOrientation)
        }
    }

    private fun getRotationMatrixFromOrientation(o: FloatArray): FloatArray {
        val xM = FloatArray(9)
        val yM = FloatArray(9)
        val zM = FloatArray(9)
        val sinX = Math.sin(o[1].toDouble()).toFloat()
        val cosX = Math.cos(o[1].toDouble()).toFloat()
        val sinY = Math.sin(o[2].toDouble()).toFloat()
        val cosY = Math.cos(o[2].toDouble()).toFloat()
        val sinZ = Math.sin(o[0].toDouble()).toFloat()
        val cosZ = Math.cos(o[0].toDouble()).toFloat()

        // rotation about x-axis (displayPitch)
        xM[0] = 1.0f
        xM[1] = 0.0f
        xM[2] = 0.0f
        xM[3] = 0.0f
        xM[4] = cosX
        xM[5] = sinX
        xM[6] = 0.0f
        xM[7] = -sinX
        xM[8] = cosX

        // rotation about y-axis (displayRoll)
        yM[0] = cosY
        yM[1] = 0.0f
        yM[2] = sinY
        yM[3] = 0.0f
        yM[4] = 1.0f
        yM[5] = 0.0f
        yM[6] = -sinY
        yM[7] = 0.0f
        yM[8] = cosY

        // rotation about z-axis (azimuth)
        zM[0] = cosZ
        zM[1] = sinZ
        zM[2] = 0.0f
        zM[3] = -sinZ
        zM[4] = cosZ
        zM[5] = 0.0f
        zM[6] = 0.0f
        zM[7] = 0.0f
        zM[8] = 1.0f

        // rotation order is y, x, z (displayRoll, displayPitch, azimuth)
        var resultMatrix: FloatArray = matrixMultiplication(xM, yM)
        resultMatrix = matrixMultiplication(zM, resultMatrix)
        return resultMatrix
    }

    private fun matrixMultiplication(A: FloatArray, B: FloatArray): FloatArray {
        val result = FloatArray(9)
        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6]
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7]
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8]
        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6]
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7]
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8]
        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6]
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7]
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8]
        return result
    }

    private fun getRotationVectorFromGyro(
        gyroValues: FloatArray,
        deltaRotationVector: FloatArray,
        timeFactor: Float
    ) {
        val normValues = FloatArray(3)

        // Calculate the angular speed of the sample
        val omegaMagnitude =
            Math.sqrt((gyroValues[0] * gyroValues[0] + gyroValues[1] * gyroValues[1] + gyroValues[2] * gyroValues[2]).toDouble())
                .toFloat()

        // Normalize the rotation vector if it's big enough to get the axis
        if (omegaMagnitude > EPSILON) {
            normValues[0] = gyroValues[0] / omegaMagnitude
            normValues[1] = gyroValues[1] / omegaMagnitude
            normValues[2] = gyroValues[2] / omegaMagnitude
        }

        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        val thetaOverTwo = omegaMagnitude * timeFactor
        val sinThetaOverTwo = Math.sin(thetaOverTwo.toDouble()).toFloat()
        val cosThetaOverTwo = Math.cos(thetaOverTwo.toDouble()).toFloat()
        deltaRotationVector[0] = sinThetaOverTwo * normValues[0]
        deltaRotationVector[1] = sinThetaOverTwo * normValues[1]
        deltaRotationVector[2] = sinThetaOverTwo * normValues[2]
        deltaRotationVector[3] = cosThetaOverTwo
    }

    inner class calculateFusedOrientationTask : TimerTask() {
        var filter_coefficient = 0.85f
        var oneMinusCoeff = 1.0f - filter_coefficient
        override fun run() {
            // Azimuth
            if (gyroOrientation[0] < -0.5 * Math.PI && accMagOrientation[0] > 0.0) {
                fusedOrientation[0] = ((filter_coefficient * (gyroOrientation[0] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[0]).toFloat())
                fusedOrientation[0] -= (if (fusedOrientation[0] > Math.PI) 2.0 * Math.PI else 0) as Float
            } else if (accMagOrientation[0] < -0.5 * Math.PI && gyroOrientation[0] > 0.0) {
                fusedOrientation[0] = ((filter_coefficient * gyroOrientation[0] + oneMinusCoeff * (accMagOrientation[0] + 2.0 * Math.PI)).toFloat())
                fusedOrientation[0] -= (if (fusedOrientation[0] > Math.PI) 2.0 * Math.PI else 0) as Float
            } else fusedOrientation[0] = filter_coefficient * gyroOrientation[0] + oneMinusCoeff * accMagOrientation[0]

            // Pitch
            if (gyroOrientation[1] < -0.5 * Math.PI && accMagOrientation[1] > 0.0) {
                fusedOrientation[1] = ((filter_coefficient * (gyroOrientation[1] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[1]).toFloat())
                fusedOrientation[1] -= (if (fusedOrientation[1] > Math.PI) 2.0 * Math.PI else 0) as Float
            } else if (accMagOrientation[1] < -0.5 * Math.PI && gyroOrientation[1] > 0.0) {
                fusedOrientation[1] = ((filter_coefficient * gyroOrientation[1] + oneMinusCoeff * (accMagOrientation[1] + 2.0 * Math.PI)).toFloat())
                fusedOrientation[1] -= (if (fusedOrientation[1] > Math.PI) 2.0 * Math.PI else 0) as Float
            } else fusedOrientation[1] =
                filter_coefficient * gyroOrientation[1] + oneMinusCoeff * accMagOrientation[1]

            // Roll
            if (gyroOrientation[2] < -0.5 * Math.PI && accMagOrientation[2] > 0.0) {
                fusedOrientation[2] = ((filter_coefficient * (gyroOrientation[2] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[2]).toFloat())
                fusedOrientation[2] -= (if (fusedOrientation[2] > Math.PI) 2.0 * Math.PI else 0) as Float
            } else if (accMagOrientation[2] < -0.5 * Math.PI && gyroOrientation[2] > 0.0) {
                fusedOrientation[2] =
                    ((filter_coefficient * gyroOrientation[2] + oneMinusCoeff * (accMagOrientation[2] + 2.0 * Math.PI)).toFloat())
                fusedOrientation[2] -= (if (fusedOrientation[2] > Math.PI) 2.0 * Math.PI else 0) as Float
            } else fusedOrientation[2] = filter_coefficient * gyroOrientation[2] + oneMinusCoeff * accMagOrientation[2]

            // Overwrite gyro matrix and orientation with fused orientation to comensate gyro drift
            gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation)
            System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3)
            pitchOut = fusedOrientation[1]
            rollOut = fusedOrientation[2]
            yawOut = fusedOrientation[0]

            // present instance values
            newPitchOut = getPitch - pitchOut
            newRollOut = getRoll - rollOut
            newYawOut = getYaw - yawOut

            // saving values for calibration
            getPitch = pitchOut
            getRoll = rollOut
            getYaw = yawOut
        }
    }

    inner class BehaviorAnalysis : TimerTask() {
        var speedLimit = 0f
        var factorSpeed = 0
        var factorBrakes = 0
        var factorAcceleration = 0
        var factorTurn = 0

        //calculate rateOverYaw and rateOverPitch by taking the division of pitch/yaw over 30 sec interval
        var rateOverPitch: Double = (finalOverPitch / count).toDouble()
        var rateOverYaw: Double = (finalOverYaw / count).toDouble()
        override fun run() {
            if (mph != 0) {
                speedLimit = mph.toFloat()
            } else {
                speedLimit = 0f
            }
            if (currentSpeed != 0F) {
                if (currentSpeed > speedLimit) {
                    factorSpeed = 10
                    runOnUiThread(Runnable {

                        val currentTimeSTamp = System.currentTimeMillis()
                        val lastTimeStamp = SpeedData.getLastTimeStamp()
                        if ((currentTimeSTamp/1000) - (lastTimeStamp/1000) > 60) {

                            val speedDataValues = SpeedData()
                            speedDataValues.speed = currentSpeed.toString()
                            speedDataValues.type = "SPEED LIMIT"
                            speedDataValues.note =
                                "You speed is above the limit, please drive within the speedlimit"
                            speedDataValues.dateTimeStamp = currentTimeSTamp
                            SpeedData.insertData(speedDataValues)
                            //"You speed is above the limit, please drive within the speedlimit"
                            playSound(speedDataValues)
                        }
                    })
                } else {
                    factorSpeed = 1
                }
                if (isBrakesApplied) {
                    factorBrakes = 10
                    runOnUiThread(Runnable {
                        val speedDataValues = SpeedData()
                        speedDataValues.speed = currentSpeed.toString()
                        speedDataValues.type = "SUDDEN BREAK"
                        speedDataValues.note = "You shouldn't apply sudden brakes, please be careful"
                        speedDataValues.dateTimeStamp = System.currentTimeMillis()
                        SpeedData.insertData(speedDataValues)
                        //"You shouldn't apply sudden brakes, please be careful"
                        playSound(speedDataValues)
                    })
                } else {
                    factorBrakes = 0
                }

                // writeCheck is the boolean used above to indicate the change in counters in turn and acc
                if (writeCheck) {
                    if (rateOverPitch < 0.04) {
                        factorAcceleration = if (xAccChange) {
                            // likely unsafe
                            8
                        } else {
                            // likely safe
                            2
                        }
                    } else {
                        if (xAccChange) {
                            // definitely unsafe
                            factorAcceleration = 10
                            runOnUiThread(Runnable {
                                val speedDataValues = SpeedData()
                                speedDataValues.speed = currentSpeed.toString()
                                speedDataValues.type = "HARSH ACCELERATION"
                                speedDataValues.note = "Harsh acceleration has been detected, please be safe"
                                speedDataValues.dateTimeStamp = System.currentTimeMillis()
                                SpeedData.insertData(speedDataValues)
                                //"Harsh acceleration has been detected, please be safe"
                                playSound(speedDataValues)
                            })
                        } else {
                            // probably unsafe
                            factorAcceleration = 8
                        }
                    }
                    factorTurn = if (rateOverYaw < 0.01) {
                        if (yAccChange) {
                            // likely unsafe
                            8
                        } else {
                            // likely safe
                            2
                        }
                    } else {
                        if (yAccChange) {
                            runOnUiThread(Runnable {
                                val speedDataValues = SpeedData()
                                speedDataValues.speed = currentSpeed.toString()
                                speedDataValues.type = "HARSH TURN"
                                speedDataValues.note = "Harsh unsafe turn has been detected, please be safe"
                                speedDataValues.dateTimeStamp = System.currentTimeMillis()
                                SpeedData.insertData(speedDataValues)
                                //"Harsh unsafe turn has been detected, please be safe"
                                playSound(speedDataValues)
                            })
                            // definitely unsafe
                            10
                        } else {
                            // probably unsafe
                            8
                        }
                    }
                } else {
                    factorAcceleration = 0
                    factorTurn = 0
                }
            }
        }
    }

    inner class ResetSensorValues : TimerTask() {
        override fun run() {
            finalOverYaw -= getFinalOverYaw
            finalOverPitch -= getFinalOverPitch
            overX -= getFinalOverX
            overY -= getFinalOverY
            getFinalOverPitch = finalOverPitch.toInt()
            getFinalOverYaw = finalOverYaw.toInt()
            getFinalOverX = overX
            getFinalOverY = overY
            Log.i("MapsActivity", "final Pitch : $finalOverPitch")
            Log.i("MapsActivity", "final Yaw : $finalOverYaw")
            Log.i("MapsActivity", "final overX : $overX")
            Log.i("MapsActivity", "final overY : $overY")
        }
    }

    private fun playSound(speedData: SpeedData) {
        initData()
        val player: MediaPlayer = MediaPlayer.create(this, Settings.System.DEFAULT_NOTIFICATION_URI)
        player.setOnCompletionListener { mediaPlayer ->
            mediaPlayer.reset()
            mediaPlayer.release()
        }
        player.start()
    }

    private fun initData() {
        val speedDataList = SpeedData.getAllData()

        if (speedDataList.isEmpty()){
            mRecyclerView.visibility = View.GONE
            mNoData.visibility = View.VISIBLE
            return
        }

        mRecyclerView.visibility = View.VISIBLE
        mNoData.visibility = View.GONE

        val layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        mRecyclerView.layoutManager = layoutManager

        mRecyclerView.adapter = MainAdapter(speedDataList)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(location: Location) {
        currentSpeed = location.speed * 2.23f
        Log.e("SENSOR_DATA", currentSpeed.toString())

        mph = speedLimit

        mSpeed.text = "Speed : $currentSpeed MPH"
        tBreakEnd = System.currentTimeMillis()
        val breakElapsed: Long = tBreakStart - tBreakEnd
        val breakElapsedSeconds = breakElapsed / 1000.0
        val breakSeconds = (breakElapsedSeconds % 60).toInt()
        if (breakSeconds % 5 == 0) {
            tempSpeed = currentSpeed
        }
        if (breakSeconds % 2 == 0 && tempSpeed >= 35 && tempSpeed - currentSpeed >= 20) {
            suddenBreaksCount++
            isBrakesApplied = true
        } else {
            isBrakesApplied = false
        }
        if (breakSeconds % 2 == 0 && currentSpeed - tempSpeed >= 20) {
            suddenAcceleration++
        }
        if (currentSpeed > mph) {
            if (flag == 0) {
                limitExceedCount++
                flag = 1
            }
        }
        if (currentSpeed < mph) {
            flag = 0
        }
        if (maxSpeed < currentSpeed) {
            maxSpeed = currentSpeed.toInt()
        }

    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

}