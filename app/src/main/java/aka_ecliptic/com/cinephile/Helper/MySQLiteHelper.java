package aka_ecliptic.com.cinephile.Helper;

public class MySQLiteHelper {

    public static final String MOVIE_TABLE = "table_of_movies";
    public static final String POSTER_TABLE = "movie_posters";
    public static final String DESCRIPTOR_TABLE = "movie_descriptors";

    //SELECTS
    public static final String SELECT_ALL_MOVIES = "SELECT * FROM 'table_of_movies'";
    public static final String SELECT_POSTERS_BY_ID = "SELECT Backdrop, ProfilePoster FROM 'movie_posters' WHERE MovieID = ?";
    public static final String SELECT_DESCRIPTORS_BY_ID = "SELECT Description FROM 'movie_descriptors' WHERE MovieID = ?";

}
