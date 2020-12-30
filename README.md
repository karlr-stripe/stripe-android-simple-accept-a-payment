# stripe-android-simple-accept-a-payment

This is intended as a very bare-bones direct implementation of the Android version of https://stripe.com/docs/payments/accept-a-payment , mainly for personal learning!

Configuration:
- If it doesn't exist, create a gradle.properties in a location defined in the Gradle Build Environment docs. For example, the default location on macOS is ~/.gradle/gradle.properties.
- Append the following entries to gradle.properties.
```
# Set to example backend deployed to Heroku
STRIPE_EXAMPLE_PAYMENTS_BACKEND_URL=https://my-backend.herokuapp.com/

# Set to a test publishable key from https://dashboard.stripe.com/test/apikeys
STRIPE_EXAMPLE_PAYMENTS_PUBLISHABLE_KEY=pk_test_mykey
``` 
Backend at https://github.com/karlr-stripe/stripe-android-simple-accept-a-payment-backend 
