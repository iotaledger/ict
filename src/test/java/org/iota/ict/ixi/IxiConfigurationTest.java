package org.iota.ict.ixi;


import org.iota.ict.ixi.context.ConfigurableIxiContext;
import org.iota.ict.ixi.context.IxiContext;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class IxiConfigurationTest {

    ConfigurableModule module;
    IxiContext contextUnderTest;
    JSONObject configuration;

    @Before
    public void setUp() {
        module = new ConfigurableModule(null);
        assertPreConditions(module);
        contextUnderTest = module.getContext();
        configuration = contextUnderTest.getConfiguration();
    }

    /**
     * Tests that a valid configuration passed to {@link IxiContext#onUpdateConfiguration(JSONObject)} will be applied.
     * */
    @Test
    public void testApplyConfiguration() {

        // given
        final String newName = "bob";
        int oldAge = configuration.getInt(ConfigurableModule.MyContext.FIELD_AGE);

        // when
        contextUnderTest.onUpdateConfiguration(configuration.put(ConfigurableModule.MyContext.FIELD_NAME, newName));

        // then
        Assert.assertEquals("New configuration was not applied on attribute 'age'.", oldAge, module.age);
        Assert.assertEquals("New configuration was not applied on attribute 'name'.", newName, module.name);
    }


    /**
     * Tests that an <b>invalid</b> configuration passed to {@link IxiContext#onUpdateConfiguration(JSONObject)} will <b>NOT</b> be applied.
     * */
    @Test
    public void testRejectInvalidConfiguration() {

        // given
        int oldAge = configuration.getInt(ConfigurableModule.MyContext.FIELD_AGE);
        final int invalidAge = 700;

        // when
        try {
            contextUnderTest.onUpdateConfiguration(configuration.put(ConfigurableModule.MyContext.FIELD_AGE, invalidAge));
            Assert.fail("No throwable was thrown when invalid configuration was passed to module.");
        } catch (IllegalArgumentException t) {}

        // then
        Assert.assertEquals("New configuration was not applied on attribute 'age'.", oldAge, module.age);
    }

    /**
     * Tests that direct modifications of the configuration returned by {@link IxiContext#getConfiguration()} are not
     * applied but require a call of {@link IxiContext#onUpdateConfiguration(JSONObject)}.
     * */
    @Test
    public void testCannotModifyConfigurationFromGetter() {

        // when
        configuration.put(ConfigurableModule.MyContext.FIELD_NAME, "bob");

        // then
        assertPreConditions(module);
    }

    /**
     * Asserts that the configuration of the Module matches the expected initial configuration state where no changes
     * have been made yet.
     * */
    private static void assertPreConditions(ConfigurableModule module) {
        IxiContext context = module.getContext();
        Assert.assertEquals("An attribute of the IXI module has unexpectedly changed.", module.name, ConfigurableModule.MyContext.DEFAULT_NAME);
        Assert.assertEquals("An attribute of the IXI module has unexpectedly changed.", module.age, ConfigurableModule.MyContext.DEFAULT_AGE);
        Assert.assertNotNull("The default configuration is not set.", context.getDefaultConfiguration());
        Assert.assertEquals("The current configuration is not the same as the default configuration", context.getDefaultConfiguration().toString(), context.getConfiguration().toString());
     }
}

// ***** IMPLEMENTATION TO TEST *****

class ConfigurableModule extends IxiModule {

    private final MyContext context  = new MyContext();

    String name = MyContext.DEFAULT_NAME;
    int age = MyContext.DEFAULT_AGE;

    ConfigurableModule(Ixi ixi) {
        super(ixi);
    }

    @Override
    public void run() { }

    @Override
    public MyContext getContext() {
        return context;
    }

    class MyContext extends ConfigurableIxiContext {

        static final String DEFAULT_NAME = "alice";
        static final int DEFAULT_AGE = 30;

        static final String FIELD_NAME = "name";
        static final String FIELD_AGE = "age";

        MyContext() {
            super(new JSONObject().put(FIELD_NAME, DEFAULT_NAME).put(FIELD_AGE, DEFAULT_AGE));
        }

        @Override
        protected void applyConfiguration() {
            name = configuration.getString(FIELD_NAME);
            age = configuration.getInt(FIELD_AGE);
        }

        @Override
        protected void validateConfiguration(JSONObject newConfiguration) {
            validateFieldName(newConfiguration);
            validateFieldAge(newConfiguration);
        }

        private void validateFieldName(JSONObject newConfiguration) {
            if (!newConfiguration.has(FIELD_NAME))
                rejectField(FIELD_NAME, "does not exist.");
            if (!(newConfiguration.get(FIELD_NAME) instanceof String))
                rejectField(FIELD_NAME, "is not a String.");
            String newName = newConfiguration.getString(FIELD_NAME);
            if (newName.length() < 3 || newName.length() > 20.)
                rejectField(FIELD_NAME, "must have a length of 3-20 characters.");
            if (!newName.matches("^[a-zA-Z0-9]*$"))
                rejectField(FIELD_NAME, "contains illegal characters");
        }

        private void validateFieldAge(JSONObject newConfiguration) {
            if (!newConfiguration.has(FIELD_AGE))
                rejectField(FIELD_AGE, "does not exist.");
            if (!(newConfiguration.get(FIELD_AGE) instanceof Integer))
                rejectField(FIELD_AGE, "is not an integer.");
            int newAge = newConfiguration.getInt(FIELD_AGE);
            if (newAge < 0 || newAge > 120)
                rejectField(FIELD_AGE, "must be in interval 0-120.");
        }

        private void rejectField(String field, String reason) {
            throw new IllegalArgumentException("Field '" + field + "' " + reason);
        }
    }
}