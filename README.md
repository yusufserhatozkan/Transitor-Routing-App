## Application Setup

1. **Download GTFS Data**
    - Download the `GTFS.zip` file for the Netherlands.
    - Import the data into your local MySQL database.

2. **Configure Database Connection**
    - Open the `DatabaseSingleton` class.
    - Update **lines 16–18** with your own MySQL credentials (host, username, password).

3. **Initial Data Cleanup**
    - On the first launch, call the `cleanData` method from `dataGetter` in the `mapGUI.main` method.
      ```java
      dataGetter.cleanData();
      ```
    - This process cleans up excess data to improve query performance.
    - **Important**: `cleanData` can be slow. Once you've run it once, comment it out or remove it from `main` to avoid delays on future runs.

## Running the Application

1. **Launch**
    - Run the `main` method in the `mapGUI` class.

2. **Usage**
    - Input two valid postcodes from the **Maastricht region**.
    - If invalid postcodes are entered, an error will be displayed.
    - If you receive an API error when fetching coordinates, try switching to:
      ```
      https://project12.ashish.nl/get_coordinates
      ```

## That's It

The application should now be ready to use for exploring public transport routes.

## Enjoy
