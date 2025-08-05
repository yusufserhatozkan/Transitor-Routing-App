package Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import Api.ApiReader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

public class ApiReaderTest {

    private ApiReader apiReader;
    private static final String VALID_ZIP_CODE = "12345";
    private static final String INVALID_ZIP_CODE = "00000";

    @Before
    public void setUp() throws Exception {        
        apiReader = Mockito.spy(new ApiReader(VALID_ZIP_CODE));
    }

    @Test
    public void testApiReaderInitialization() throws Exception {        
        assertNotNull(apiReader);        
        assertEquals(50.8503, apiReader.getLatitude(), 0.0001);
        assertEquals(4.3517, apiReader.getLongitude(), 0.0001);
    }

    @Test
    public void testInvalidZipCodeHandling() throws Exception {
        ApiReader invalidApiReader = new ApiReader(INVALID_ZIP_CODE);
        assertNotNull(invalidApiReader);
    }
}

