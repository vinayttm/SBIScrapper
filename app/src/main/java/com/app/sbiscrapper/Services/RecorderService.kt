package com.app.sbiscrapper.Services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.app.sbiscrapper.ApiManager.ApiManager
import com.app.sbiscrapper.Config
import com.app.sbiscrapper.MainActivity
import com.app.sbiscrapper.R
import com.app.sbiscrapper.Utils.AES
import com.app.sbiscrapper.Utils.AccessibilityUtil
import com.app.sbiscrapper.Utils.AutoRunner
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.reflect.Field
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Locale


class RecorderService : AccessibilityService() {
    private val ticker = AutoRunner(this::initialStage)
    private var appNotOpenCounter = 0
    private val apiManager = ApiManager()
    private val au = AccessibilityUtil()
    private var aes = AES()
    private var isAccountSummary = false
    private var isTransactionAccount = false
    private var isGetBalance = false
    private var totalBalance = ""
    private var isTransactionAccountDetails = false
    private var isStatement = false


    override fun onServiceConnected() {
        super.onServiceConnected()
        ticker.startRunning()
    }

    override fun onAccessibilityEvent(p0: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
    }

    private fun initialStage() {
        Log.d("initialStage", "initialStage  Event")
        printAllFlags().let { Log.d("Flags", it) }
        ticker.startReAgain()
        if (!MainActivity().isAccessibilityServiceEnabled(this, this.javaClass)) {
            return;
        }
        val rootNode: AccessibilityNodeInfo? = au.getTopMostParentNode(rootInActiveWindow)
        if (rootNode != null) {
            if (au.findNodeByPackageName(rootNode, Config.packageName) == null) {
                if (appNotOpenCounter > 4) {
                    Log.d("App Status", "Not Found")
                    relaunchApp()
                    try {
                        Thread.sleep(4000)
                    } catch (e: InterruptedException) {
                        throw RuntimeException(e)
                    }
                    appNotOpenCounter = 0
                    return
                }
                appNotOpenCounter++
            } else {
                checkForSessionExpiry()
                apiManager.checkUpiStatus { isActive ->
                    if (!isActive) {
                        closeAndOpenApp()
                    }
                    else
                    {
                        checkForSessionExpiry()
                        enterPin()
                        accounts()
                        accountSummary()
                        transactionAccount()
                        transactionAccountDetails()
                        getBalance()
                        getMiniStatement()
                    }
                }
                au.listAllTextsInActiveWindow(au.getTopMostParentNode(rootInActiveWindow))
            }
            rootNode.recycle()
        }
    }

    private fun closeAndOpenApp() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }


    private fun enterPin() {
        val corporateString = au.findNodeByText(
            rootInActiveWindow, "Corporate", false, false
        )
        corporateString.apply {

            val usernameNode = au.findNodeByText(
                rootInActiveWindow, "Username", false, false
            )
            val passwordNode = au.findNodeByText(
                rootInActiveWindow, "**********", false, false
            )
            if (usernameNode != null && passwordNode != null) {
                val userNameBundle = Bundle()
                userNameBundle.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    Config.bankLoginId
                )
                usernameNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, userNameBundle)
                usernameNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)


                val passwordBundle = Bundle()
                passwordBundle.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    Config.loginPin
                )
                passwordNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, passwordBundle)
                passwordNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)

                val login = au.findNodeByText(
                    rootInActiveWindow, "Login", false, false
                )
                login?.apply {
                    performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
            }
        }
    }

    private fun accounts() {
        val accounts =
            au.findNodeByText(
                rootInActiveWindow, "Accounts", false, false
            )
        accounts?.apply {
            val outBounds = Rect()
            accounts.getBoundsInScreen(outBounds)
            performTap(outBounds.centerX(), outBounds.centerY())
        }
    }


    private fun accountSummary() {
        if (isAccountSummary) {
            return
        }
        val accountSummaryNode = au.findNodeByText(
            rootInActiveWindow, "Account Summary", false, false
        )
        if (accountSummaryNode != null) {
            val outBounds = Rect()
            accountSummaryNode.getBoundsInScreen(outBounds)
            val isClicked = performTap(outBounds.centerX(), outBounds.centerY())
            if (isClicked) {
                accountSummaryNode.recycle()
                isAccountSummary = true
            }
        }
        try {
            Thread.sleep(500)
        } catch (e: InterruptedException) {
            throw java.lang.RuntimeException(e)
        }
    }


    private fun transactionAccount() {
        checkForSessionExpiry()
        if (isTransactionAccount) {
            return
        }
        try {
            Thread.sleep(3000)
        } catch (e: InterruptedException) {
            throw java.lang.RuntimeException(e)
        }
        val transactionAccount = au.findNodeByText(
            rootInActiveWindow, "(SB/CA/OD Accounts)", true, false
        )
        if (transactionAccount != null) {
            val outBounds = Rect()
            transactionAccount.getBoundsInScreen(outBounds)
            val isClicked: Boolean = performTap(
                outBounds.centerX(),
                outBounds.centerY()
            )
            if (isClicked) {
                transactionAccount.recycle()
                isTransactionAccount = true
                isTransactionAccountDetails = true
                isAccountSummary = true
            }
        }
        try {
            Thread.sleep(3000)
        } catch (e: InterruptedException) {
            throw java.lang.RuntimeException(e)
        }
    }

    private fun transactionAccountDetails() {
        checkForSessionExpiry()
        if (isTransactionAccountDetails) {
            try {
                Thread.sleep(2000)
            } catch (e: InterruptedException) {
                throw java.lang.RuntimeException(e)
            }
            val isClicked: Boolean = performTap(317, 389)
            if (isClicked) {
                println("transactionAccountDetails $isTransactionAccountDetails")
                isTransactionAccountDetails = true
            }
        }
    }


    private fun getBalance() {
        if (isGetBalance) {
            return
        }
        val currentBalanceList: List<String> = au.listAllTextsInActiveWindow(rootInActiveWindow)
        val availableBalanceIndex = currentBalanceList.indexOf("Available Balance")
        if (availableBalanceIndex != -1 && availableBalanceIndex < currentBalanceList.size - 1) {
            var availableBalanceValue = currentBalanceList[availableBalanceIndex + 1]
            availableBalanceValue =
                availableBalanceValue.replace(",".toRegex(), "").replace("Rs\\.\\s".toRegex(), "")
            println("Available Balance: $availableBalanceValue")
            totalBalance = availableBalanceValue
        } else {
            println("Available Balance not found or value not available.")
        }
        val accountInformation = au.findNodeByText(
            au.getTopMostParentNode(
                rootInActiveWindow
            ), "Account Information", true, false
        )
        if (accountInformation != null) {
            val outBounds = Rect()
            accountInformation.getBoundsInScreen(outBounds)
            val isClicked = performTap(outBounds.centerX(), outBounds.centerY())
            if (isClicked) {
                accountInformation.recycle()
                isGetBalance = true
                isStatement = false
                isTransactionAccountDetails = true
                isTransactionAccount = true
            }
        }
        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            throw java.lang.RuntimeException(e)
        }
    }

    private var scrollCounter = 0

    private fun getMiniStatement() {
        if (totalBalance.isEmpty()) {
            isGetBalance = false
        } else {
            Log.d("Total Balance", "Total Balance=> " + totalBalance)
            val miniStatement = au.findNodeByText(
                au.getTopMostParentNode(
                    rootInActiveWindow
                ), "Mini Statement", true, false
            )
            if (!isStatement) {
                if (miniStatement != null) {
                    val outBounds = Rect()
                    miniStatement.getBoundsInScreen(outBounds)
                    val isClicked = performTap(outBounds.centerX(), outBounds.centerY())
                    if (isClicked) {
                        isStatement = true
                    }
                }
            }
            try {
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
                throw java.lang.RuntimeException(e)
            }
            readTransaction()
        }
    }

    private fun backingProcess() {
        val accountSummary = au.findNodeByText(rootInActiveWindow, "Account Summary", false, false)
        accountSummary?.apply {
            performTap(42, 120)
            isStatement = false
            isGetBalance = false
            scrollCounter = 0;
            totalBalance = ""
            isTransactionAccount = true
            isTransactionAccountDetails = true

        }
    }


    private fun filterList(): MutableList<String> {
        val mainList = au.listAllTextsInActiveWindow(au.getTopMostParentNode(rootInActiveWindow))
        val mutableList = mutableListOf<String>()
        if (mainList.contains("Mini Statement")) {
            val unfilteredList = mainList.filter { it.isNotEmpty() }
            val miniIndex = unfilteredList.indexOf("Mini Statement")
            val welcomeIndex = unfilteredList.indexOf("Welcome  ")
            val separatedList =
                unfilteredList.subList(miniIndex, welcomeIndex).toMutableList()
            separatedList.removeAt(0)
            println("modifiedList $separatedList")
            mutableList.addAll(separatedList)

        }
        return mutableList
    }

    private fun readTransaction() {
        try {
            val output = JSONArray()
            val mainList = au.listAllTextsInActiveWindow(au.getTopMostParentNode(rootInActiveWindow))
            if (mainList.contains("Mini Statement")) {
                val filterList = filterList();
                println("filterList = $filterList")
                for (i in filterList.indices step 3) {
                    val date = filterList[i]
                    if (isDate(date)) {
                        val description = filterList[i + 1]
                        var amount = filterList[i + 2]
                        if (amount.contains("(Cr)")) {
                            amount = amount.replace("(Cr)", "");
                            amount = amount.replace(" ", "");
                        }
                        if (amount.contains("(Dr)")) {
                            amount = amount.replace("(Dr)", "");
                            amount = amount.replace(",", "");
                            amount = amount.replace(" ", "");
                            amount = "-$amount";
                        }
                        if (amount.contains("Rs.") || amount.contains("Rs")) {
                            amount = amount.replace("Rs.", "");
                            amount = amount.replace(",", "");
                        }

                        val entry = JSONObject()
                        try {
                            entry.put("Amount", amount.replace(",", ""))
                            entry.put("RefNumber", extractUTRFromDesc(description))
                            entry.put("Description", extractUTRFromDesc(description))
                            entry.put("AccountBalance", totalBalance.replace(",", ""))
                            entry.put("CreatedDate", date)
                            entry.put("BankName", Config.bankName + Config.bankLoginId)
                            entry.put("BankLoginId", Config.bankLoginId)
                            entry.put("UPIId", getUPIId(description))
                            output.put(entry)
                        } catch (e: JSONException) {
                            throw java.lang.RuntimeException(e)
                        }


                    }
                }
                Log.d("Final Json Output", output.toString());
                Log.d("Total length", output.length().toString());
                if (output.length() > 0) {
                    val result = JSONObject()
                    try {
                        result.put("Result", aes.encrypt(output.toString()))
                        apiManager.saveBankTransaction(result.toString());
                        backingProcess()
                        Thread.sleep(5000)
                    } catch (e: JSONException) {
                        throw java.lang.RuntimeException(e)
                    }
                }

            }

        }catch (ignored: Exception) {
        }
    }


    private fun isDate(dateString: String?): Boolean {
        val dateFormat = SimpleDateFormat("dd-MMM-yyyy")
        dateFormat.isLenient = false
        return try {
            val parsedDate = dateFormat.parse(dateString)
            true
        } catch (e: ParseException) {
            false
        }
    }


    private val queryUPIStatus = Runnable {
        val intent = packageManager.getLaunchIntentForPackage(Config.packageName)
        intent?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(this)
        }
    }
    private val inActive = Runnable {
        Toast.makeText(this, "UjjivanBankScrapper inactive", Toast.LENGTH_LONG).show();
    }

    private fun relaunchApp() {
        apiManager.queryUPIStatus(queryUPIStatus, inActive)
    }

    private fun formatDate(inputDateString: String): String {
        val inputFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        val outputFormat = SimpleDateFormat("d/M/yyyy", Locale.ENGLISH)

        val date = inputFormat.parse(inputDateString)
        return outputFormat.format(date!!)
    }

    private fun checkForSessionExpiry() {
        val targetNode1 = au.findNodeByText(
            au.getTopMostParentNode(
                rootInActiveWindow
            ), "Do you really want to Logout?", true, false
        )
        val targetNode2 = au.findNodeByText(
            au.getTopMostParentNode(
                rootInActiveWindow
            ), "Unable to process the request", false, false
        )
        val targetNode3 = au.findNodeByText(
            au.getTopMostParentNode(
                rootInActiveWindow
            ), "Feedback", true, false
        )
        val targetNode4 = au.findNodeByText(
            au.getTopMostParentNode(
                rootInActiveWindow
            ), "Dear Customer", true, false
        )
        val targetNode5 = au.findNodeByText(
            au.getTopMostParentNode(
                rootInActiveWindow
            ), "Unable to process the request, Please try again.", true, false
        )
        val targetNode6 = au.findNodeByText(
            au.getTopMostParentNode(
                rootInActiveWindow
            ), "Logout", true, false
        )
        val targetNode7 = au.findNodeByText(
            au.getTopMostParentNode(
                rootInActiveWindow
            ), "No Accounts found for Deposit Account. ", true, false
        )
        val targetNode8 = au.findNodeByText(
            au.getTopMostParentNode(
                rootInActiveWindow
            ),
            "Due to some technical problems we are unable to process your request. Please try later.",
            true,
            false
        )
        val targetNode9 = au.findNodeByText(
            au.getTopMostParentNode(
                rootInActiveWindow
            ), "Unable to retrieve last 5 transactions. Please try later.", true, false
        )
        val targetNode10 = au.findNodeByText(
            au.getTopMostParentNode(
                rootInActiveWindow
            ),
            "You have already logged in. In case you have not logged out of your earlier session please try later.",
            true,
            false
        )
        val targetNode11 = au.findNodeByText(
            au.getTopMostParentNode(
                rootInActiveWindow
            ),
            "Invalid reference no or session already expired",
            true,
            false
        )

        //Invalid reference no or session already expired
        if (targetNode1 != null) {
            val logoutButton = au.findNodeByText(
                au.getTopMostParentNode(
                    rootInActiveWindow
                ), "YES", true, true
            )
            if (logoutButton != null) {
                val outBounds = Rect()
                logoutButton.getBoundsInScreen(outBounds)
                performTap(outBounds.centerX(), outBounds.centerY())
                logoutButton.recycle()
            }
        }
        if (targetNode2 != null) {
            val button = au.findNodeByText(
                au.getTopMostParentNode(
                    rootInActiveWindow
                ), "OK", true, true
            )
            if (button != null) {
                val outBounds = Rect()
                button.getBoundsInScreen(outBounds)
                performTap(outBounds.centerX(), outBounds.centerY())
                button.recycle()
            }
        }
        if (targetNode3 != null) {
            val button = au.findNodeByText(
                au.getTopMostParentNode(
                    rootInActiveWindow
                ), "NEXT TIME", true, true
            )
            if (button != null) {
                val outBounds = Rect()
                button.getBoundsInScreen(outBounds)
                performTap(outBounds.centerX(), outBounds.centerY())
                button.recycle()
                isStatement = false
                isTransactionAccount = false
                isAccountSummary = false
                isGetBalance = false
                // scrollCounter = 0
                ticker.startReAgain()
            }
        }
        if (targetNode4 != null) {
            val button = au.findNodeByText(
                au.getTopMostParentNode(
                    rootInActiveWindow
                ), "OK", true, true
            )
            if (button != null) {
                val outBounds = Rect()
                button.getBoundsInScreen(outBounds)
                performTap(outBounds.centerX(), outBounds.centerY())
                button.recycle()
                ticker.startReAgain()
            }
        }
        if (targetNode5 != null) {
            val button = au.findNodeByText(
                au.getTopMostParentNode(
                    rootInActiveWindow
                ), "OK", true, true
            )
            if (button != null) {
                val outBounds = Rect()
                button.getBoundsInScreen(outBounds)
                performTap(outBounds.centerX(), outBounds.centerY())
                button.recycle()
                totalBalance = ""
                ticker.startReAgain()
            }
        }
        if (targetNode6 != null) {
            val button = au.findNodeByText(
                au.getTopMostParentNode(
                    rootInActiveWindow
                ), "OK", true, true
            )
            if (button != null) {
                val outBounds = Rect()
                button.getBoundsInScreen(outBounds)
                val isClicked = performTap(outBounds.centerX(), outBounds.centerY())
                if (isClicked) {
                    isStatement = false
                    isTransactionAccount = false
                    isAccountSummary = false
                    isGetBalance = false
                    isTransactionAccountDetails = false
                    totalBalance = ""
                    ticker.startReAgain()
                }
            }
        }
        if (targetNode7 != null) {
            val button = au.findNodeByText(
                au.getTopMostParentNode(
                    rootInActiveWindow
                ), "OK", true, true
            )
            if (button != null) {
                val outBounds = Rect()
                button.getBoundsInScreen(outBounds)
                performTap(outBounds.centerX(), outBounds.centerY())
                button.recycle()
                ticker.startReAgain()
            }
        }
        if (targetNode8 != null) {
            val button = au.findNodeByText(
                au.getTopMostParentNode(
                    rootInActiveWindow
                ), "OK", true, true
            )
            if (button != null) {
                val outBounds = Rect()
                button.getBoundsInScreen(outBounds)
                performTap(outBounds.centerX(), outBounds.centerY())
                button.recycle()
                ticker.startReAgain()
            }
        }
        if (targetNode9 != null) {
            val button = au.findNodeByText(
                au.getTopMostParentNode(
                    rootInActiveWindow
                ), "OK", true, true
            )
            if (button != null) {
                val outBounds = Rect()
                button.getBoundsInScreen(outBounds)
                performTap(outBounds.centerX(), outBounds.centerY())
                button.recycle()
                totalBalance = ""
                ticker.startReAgain()
            }
        }
        if (targetNode10 != null) {
            val button = au.findNodeByText(
                au.getTopMostParentNode(
                    rootInActiveWindow
                ), "OK", true, true
            )
            if (button != null) {
                val outBounds = Rect()
                button.getBoundsInScreen(outBounds)
                performTap(outBounds.centerX(), outBounds.centerY())
                button.recycle()
                ticker.startReAgain()
            }
        }
        if (targetNode11 != null) {
            val button = au.findNodeByText(
                au.getTopMostParentNode(
                    rootInActiveWindow
                ), "OK", true, true
            )
            if (button != null) {
                val outBounds = Rect()
                button.getBoundsInScreen(outBounds)
                performTap(outBounds.centerX(), outBounds.centerY())
                ticker.startReAgain()
            }
        }

    }

    private fun sendNotification(message: String) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "MyAccessibilityServiceChannel"
            val channel = NotificationChannel(
                channelId,
                "Accessibility Service Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications from the Accessibility Service"
                enableLights(true)
                lightColor = Color.BLUE
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, "MyAccessibilityServiceChannel")
            .setSmallIcon(androidx.core.R.drawable.notification_bg)
            .setContentTitle("Accessibility Service")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(1, notificationBuilder.build())
    }


    private fun performTap(x: Float, y: Float, duration: Long) {
        Log.d("Accessibility", "Tapping $x and $y")
        val p = Path()
        p.moveTo(x, y)
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(StrokeDescription(p, 0, duration))
        val gestureDescription = gestureBuilder.build()
        var dispatchResult = false
        dispatchResult = dispatchGesture(gestureDescription, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                super.onCompleted(gestureDescription)
            }
        }, null)
        Log.d("Dispatch Result", dispatchResult.toString())
    }

    private fun getUPIId(description: String): String {
        if (!description.contains("@")) return ""
        val split: Array<String?> =
            description.split("/".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
        var value: String? = null
        value = Arrays.stream(split).filter { x: String? ->
            x!!.contains(
                "@"
            )
        }.findFirst().orElse(null)
        return value ?: ""

    }

    private fun extractUTRFromDesc(description: String): String? {
        return try {
            val split: Array<String?> =
                description.split("/".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            var value: String? = null
            value = Arrays.stream(split).filter { x: String? -> x!!.length == 12 }
                .findFirst().orElse(null)
            if (value != null) {
                "$value $description"
            } else description
        } catch (e: Exception) {
            description
        }
    }


    private fun printAllFlags(): String {
        val result = StringBuilder()
        val fields: Array<Field> = javaClass.declaredFields
        for (field in fields) {
            field.isAccessible = true
            val fieldName: String = field.name
            try {
                val value: Any? = field.get(this)
                result.append(fieldName).append(": ").append(value).append("\n")
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
        return result.toString()
    }

    private fun performTap(x: Int, y: Int): Boolean {
        Log.d("Accessibility", "Tapping $x and $y")
        val p = Path()
        p.moveTo(x.toFloat(), y.toFloat())
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(StrokeDescription(p, 0, 950))
        val gestureDescription = gestureBuilder.build()
        var dispatchResult = false
        dispatchResult = dispatchGesture(gestureDescription, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                super.onCompleted(gestureDescription)
            }
        }, null)
        Log.d("Dispatch Result", dispatchResult.toString())
        return dispatchResult
    }

}