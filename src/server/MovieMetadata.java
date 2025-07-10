package server;

public class MovieMetadata {
    public String filename;
    public long size;
    public int duration;

    public MovieMetadata(String filename, long size, int duration) {
        this.filename = filename;
        this.size = size;
        this.duration = duration;
    }
}
