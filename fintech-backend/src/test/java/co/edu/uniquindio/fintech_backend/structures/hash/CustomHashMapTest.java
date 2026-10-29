package co.edu.uniquindio.fintech_backend.structures.hash;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomHashMapTest {
    private CustomHashMap<String, Integer> map;

    @BeforeEach
    void setUp() {
        map = new CustomHashMap<>();
    }

    @Test
    void shouldInsertAndRetrieveElements() {
        // Arrange
        map.put("Carlos", 90);
        map.put("Pedro", 85);
        map.put("Ana", 95);
        // Act
        Integer carlosScore = map.get("Carlos");
        Integer pedroScore = map.get("Pedro");
        Integer anaScore = map.get("Ana");
        // Assert
        assertEquals(90, carlosScore);
        assertEquals(85, pedroScore);
        assertEquals(95, anaScore);
        assertEquals(3, map.size());
        assertFalse(map.isEmpty());
    }

    @Test
    void shouldUpdateExistingKeyWithoutIncreasingSize() {
        // Arrange
        map.put("Carlos", 90);
        // Act
        Integer previousValue = map.put("Carlos", 95);
        // Assert
        assertEquals(90, previousValue);
        assertEquals(95, map.get("Carlos"));
        assertEquals(1, map.size());
        assertFalse(map.isEmpty());
    }

    @Test
    void shouldHandleHashCollisions() {
        // Arrange
        String key1 = "FB";
        String key2 = "Ea";
        assertNotEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
        // Act
        map.put(key1, 10);
        map.put(key2, 20);
        // Assert
        assertEquals(10, map.get(key1));
        assertEquals(20, map.get(key2));
        assertTrue(map.containsKey(key1));
        assertTrue(map.containsKey(key2));
        assertEquals(2, map.size());
    }

    @Test
    void shouldRemoveFirstNodeFromBucket() {
        // Arrange
        map.put("FB", 10);
        map.put("Ea", 20);
        // Act
        Integer removedValue = map.remove("Ea");
        // Assert
        assertEquals(20, removedValue);
        assertNull(map.get("Ea"));
        assertFalse(map.containsKey("Ea"));
        assertEquals(10, map.get("FB"));
        assertTrue(map.containsKey("FB"));
        assertEquals(1, map.size());
        assertFalse(map.isEmpty());
    }

    @Test
    void shouldRemoveMiddleNodeFromBucket() {
        // Arrange
        String key1 = "AaAa";
        String key2 = "BBBB";
        String key3 = "AaBB";
        assertEquals(key1.hashCode(), key2.hashCode());
        assertEquals(key2.hashCode(), key3.hashCode());
        map.put(key1, 10);
        map.put(key2, 20);
        map.put(key3, 30);
        // Act
        Integer removedValue = map.remove(key2);
        // Assert
        assertEquals(20, removedValue);
        assertNull(map.get(key2));
        assertFalse(map.containsKey(key2));
        assertEquals(10, map.get(key1));
        assertEquals(30, map.get(key3));
        assertEquals(2, map.size());
    }

    @Test
    void shouldRemoveLastNodeFromBucket() {
        // Arrange
        String key1 = "AaAa";
        String key2 = "BBBB";
        String key3 = "AaBB";
        map.put(key1, 10);
        map.put(key2, 20);
        map.put(key3, 30);
        // Act
        Integer removedValue = map.remove(key1);
        // Assert
        assertEquals(10, removedValue);
        assertNull(map.get(key1));
        assertFalse(map.containsKey(key1));
        assertEquals(20, map.get(key2));
        assertEquals(30, map.get(key3));
        assertEquals(2, map.size());
        assertFalse(map.isEmpty());
    }

    @Test
    void shouldReturnNullWhenRemovingNonexistentKey() {
        // Arrange
        map.put("Carlos", 90);
        map.put("Pedro", 85);
        // Act
        Integer removedValue = map.remove("Ana");
        // Assert
        assertNull(removedValue);
        assertEquals(90, map.get("Carlos"));
        assertEquals(85, map.get("Pedro"));
        assertEquals(2, map.size());
        assertFalse(map.isEmpty());
    }

    @Test
    void shouldResizeAndPreserveAllElements() {
        // Arrange
        assertEquals(16, map.capacity());
        for (int i = 1; i <= 12; i++) {
            map.put("User" + i, i * 10);
        }
        assertEquals(12, map.size());
        assertEquals(16, map.capacity());
        // Act
        map.put("User13", 130);
        // Assert
        assertEquals(32, map.capacity());
        assertEquals(13, map.size());
        for (int i = 1; i <= 13; i++) {
            assertEquals(i * 10, map.get("User" + i));
        }
    }

    @Test
    void shouldNotResizeWhenUpdatingExistingKey() {
        // Arrange
        for (int i = 1; i <= 12; i++) {
            map.put("User" + i, i * 10);
        }
        assertEquals(12, map.size());
        assertEquals(16, map.capacity());
        // Act
        Integer previousValue = map.put("User5", 999);
        // Assert
        assertEquals(50, previousValue);
        assertEquals(999, map.get("User5"));
        assertEquals(12, map.size());
        assertEquals(16, map.capacity());
    }

    @Test
    void shouldVisitAllValuesAfterResizeAndCollision() {
        // Arrange
        for (int i = 1; i <= 13; i++) {
            map.put("User" + i, i);
        }
        map.put("FB", 100);
        map.put("Ea", 200);
        boolean[] visited = new boolean[201];
        int[] count = {0};
        // Act
        map.forEachValue(value -> {
            assertFalse(visited[value]);
            visited[value] = true;
            count[0]++;
        });
        // Assert
        assertEquals(15, count[0]);
        assertEquals(15, map.size());
        for (int i = 1; i <= 13; i++) {
            assertTrue(visited[i]);
        }
        assertTrue(visited[100]);
        assertTrue(visited[200]);
    }

    @Test
    void shouldRejectNullKeysAndValues() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> map.put(null, 90));
        assertThrows(NullPointerException.class, () -> map.put("Carlos", null));
        assertThrows(NullPointerException.class, () -> map.get(null));
        assertThrows(NullPointerException.class, () -> map.containsKey(null));
        assertThrows(NullPointerException.class, () -> map.remove(null));
        // The map must remain empty
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    void shouldPreserveElementsAfterMultipleResizes() {
        // Arrange & Act
        for (int i = 1; i <= 100; i++) {
            map.put("User" + i, i * 10);
            if (i == 13) {
                assertEquals(32, map.capacity());
            }
            if (i == 25) {
                assertEquals(64, map.capacity());
            }
            if (i == 49) {
                assertEquals(128, map.capacity());
            }
            if (i == 97) {
                assertEquals(256, map.capacity());
            }
        }
        // Assert
        assertEquals(100, map.size());
        assertEquals(256, map.capacity());
        for (int i = 1; i <= 100; i++) {
            assertEquals(i * 10, map.get("User" + i));
        }
    }
}
