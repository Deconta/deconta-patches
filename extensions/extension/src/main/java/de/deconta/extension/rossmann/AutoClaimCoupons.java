package de.deconta.extension.rossmann;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AutoClaimCoupons {

    private static final String TAG = "DecontaPatches";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final long ACTIVATION_DELAY_MS = 350;

    private static final Set<String> claimedCoupons = Collections.synchronizedSet(new HashSet<String>());
    private static boolean isProcessing = false;

    private AutoClaimCoupons() {
    }

    public static void onCouponsLoaded(final Object fragment, final Object data) {
        if (fragment == null || data == null) {
            return;
        }

        MAIN.post(new Runnable() {
            @Override
            public void run() {
                try {
                    processCoupons(fragment, data);
                } catch (Throwable t) {
                    Log.e(TAG, "Error in onCouponsLoaded", t);
                }
            }
        });
    }

    private static void processCoupons(Object fragment, Object data) throws Exception {
        if (isProcessing) {
            return;
        }

        Method getContextMethod = fragment.getClass().getMethod("getContext");
        final Context context = (Context) getContextMethod.invoke(fragment);
        if (context == null) {
            return;
        }

        Field factoryField = null;
        for (Field f : fragment.getClass().getFields()) {
            if (f.getType().getName().contains("CouponPresenterFactory")) {
                factoryField = f;
                break;
            }
        }
        if (factoryField == null) {
            factoryField = fragment.getClass().getField("p");
        }

        Object factory = factoryField.get(fragment);
        if (factory == null) {
            Log.e(TAG, "CouponPresenterFactory is null");
            return;
        }

        Method createPresenterMethod = factory.getClass().getMethod("a");
        final Object presenter = createPresenterMethod.invoke(factory);
        if (presenter == null) {
            Log.e(TAG, "CouponPresenter is null");
            return;
        }

        Class<?> displayInterface = Class.forName("de.rossmann.app.android.ui.coupon.CouponDisplay");
        Object dummyDisplay = Proxy.newProxyInstance(
            displayInterface.getClassLoader(),
            new Class<?>[]{displayInterface},
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    if ("m".equals(method.getName())) {
                        return Boolean.FALSE;
                    }
                    if (method.getReturnType().equals(boolean.class)) {
                        return Boolean.FALSE;
                    }
                    if (method.getReturnType().equals(void.class)) {
                        return null;
                    }
                    return null;
                }
            }
        );

        Field displayField = null;
        Class<?> current = presenter.getClass();
        while (current != null && displayField == null) {
            for (Field f : current.getDeclaredFields()) {
                if (f.getType().equals(displayInterface) || f.getName().equals("f41627a")) {
                    displayField = f;
                    break;
                }
            }
            current = current.getSuperclass();
        }
        if (displayField != null) {
            displayField.setAccessible(true);
            displayField.set(presenter, dummyDisplay);
        }

        List<Object> allCoupons = new ArrayList<>();

        for (Field f : data.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            Object val = f.get(data);
            if (val instanceof List) {
                for (Object item : (List<?>) val) {
                    if (item != null && item.getClass().getName().contains("CouponDisplayModel")) {
                        allCoupons.add(item);
                    }
                }
            }
        }

        if (allCoupons.isEmpty()) {
            return;
        }

        final List<Object> toClaim = new ArrayList<>();
        Method getCouponIdMethod = null;
        Method isInWalletMethod = null;
        Method showAsExclusiveMethod = null;
        Method isRedeemedMethod = null;

        for (Object coupon : allCoupons) {
            Class<?> couponCls = coupon.getClass();
            if (getCouponIdMethod == null) {
                getCouponIdMethod = couponCls.getMethod("getCouponId");
                isInWalletMethod = couponCls.getMethod("isInWallet");
                showAsExclusiveMethod = couponCls.getMethod("showAsExclusive");
                isRedeemedMethod = couponCls.getMethod("isRedeemed");
            }

            String id = (String) getCouponIdMethod.invoke(coupon);
            boolean inWallet = (Boolean) isInWalletMethod.invoke(coupon);
            boolean exclusive = (Boolean) showAsExclusiveMethod.invoke(coupon);
            boolean redeemed = (Boolean) isRedeemedMethod.invoke(coupon);

            if (!inWallet && !exclusive && !redeemed && id != null) {
                if (!claimedCoupons.contains(id)) {
                    toClaim.add(coupon);
                }
            }
        }

        if (toClaim.isEmpty()) {
            return;
        }

        Log.i(TAG, "Found " + toClaim.size() + " coupons to auto-activate");

        Method activateMethodCandidate = null;
        for (Method m : presenter.getClass().getDeclaredMethods()) {
            Class<?>[] params = m.getParameterTypes();
            if (params.length == 2 && params[0].getName().contains("CouponDisplayModel") && Context.class.isAssignableFrom(params[1])) {
                activateMethodCandidate = m;
                break;
            }
        }
        if (activateMethodCandidate == null) {
            Log.e(TAG, "Activation method j(CouponDisplayModel, Context) not found on presenter");
            return;
        }
        final Method activateMethod = activateMethodCandidate;
        activateMethod.setAccessible(true);

        isProcessing = true;
        final int totalCount = toClaim.size();

        processQueue(0, toClaim, presenter, activateMethod, context, getCouponIdMethod, totalCount);
    }

    private static void processQueue(
        final int index,
        final List<Object> queue,
        final Object presenter,
        final Method activateMethod,
        final Context context,
        final Method getCouponIdMethod,
        final int totalCount
    ) {
        if (index >= queue.size()) {
            isProcessing = false;
            MAIN.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(
                        context,
                        "DecontaPatches: " + totalCount + " Coupons automatisch aktiviert!",
                        Toast.LENGTH_SHORT
                    ).show();
                }
            });
            return;
        }

        Object coupon = queue.get(index);
        try {
            String id = (String) getCouponIdMethod.invoke(coupon);
            if (id != null) {
                claimedCoupons.add(id);
            }
            activateMethod.invoke(presenter, coupon, context);
            Log.d(TAG, "Activated coupon: " + id);
        } catch (Throwable t) {
            Log.w(TAG, "Failed activating coupon index " + index, t);
        }

        MAIN.postDelayed(new Runnable() {
            @Override
            public void run() {
                processQueue(index + 1, queue, presenter, activateMethod, context, getCouponIdMethod, totalCount);
            }
        }, ACTIVATION_DELAY_MS);
    }
}
