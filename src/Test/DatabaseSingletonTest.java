package Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import Data.DatabaseSingleton;

public class DatabaseSingletonTest {

    private Connection mockConnection;

    @Before
    public void setUp() throws SQLException {        
        mockConnection = mock(Connection.class);
    }

    @Test
    public void testGetInstance() throws SQLException {
        try (MockedStatic<DriverManager> mockedDriverManager = Mockito.mockStatic(DriverManager.class)) {
            mockedDriverManager.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                               .thenReturn(mockConnection);
            
            Connection instance1 = DatabaseSingleton.getConnection();
            Connection instance2 = DatabaseSingleton.getConnection();            
            assertSame(instance1, instance2);                    
            mockedDriverManager.verify(() -> DriverManager.getConnection(anyString(), anyString(), anyString()), times(1));
        }
    }
}
