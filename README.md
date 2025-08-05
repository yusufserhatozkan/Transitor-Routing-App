## Application Setup

1. **Download GTFS Data**
    - Ensure that you have the GTFS.zip file for the Netherlands downloaded.
    - Import this data into your MySQL database.

2. **Configure Database Connection**
    - Open the `DatabaseSingleton` class.
    - Update lines 16, 17, and 18 with your MySQL database details.

3. **Initial Data Cleanup**
    - The first time you launch the application, you may wish to call the `cleanData` method in the `mapGUI.main` method: `dataGetter.cleanData`'
    - This method will clear any unnecessary data from your database and ensure speedy query times.
    - **Note:** Running `cleanData` takes some time, so after you have used it to clear your database, you should remove it from the `main` method for future launches to speed up the startup process.

## Running the Application

1. **Launch the Application**
    - Run the `main` method in the `mapGUI` class to launch the application.

2. **Using the Application**
    - The application allows you to input two postcodes. It only accepts postcodes within the vicinity of Maastricht.
    - If you do not input appropriate postcodes, it will display an error message.
    - **Note:** If the API throws an error, you may wish to look for an alternative API to call from. Try `https://project12.ashish.nl/get_coordinates`.

## Enjoy!
