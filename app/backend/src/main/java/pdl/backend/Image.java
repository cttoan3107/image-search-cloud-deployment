/*
  Global role of this class:

  - Represents an image entity in the system
  - Stores image ID, name, and binary data
*/
package pdl.backend;

public class Image {
  private Long id;
  private String name;
  private byte[] data;

  public Image(final Long id, final String name, final byte[] data) {
    this.id = id;
    this.name = name;
    this.data = data;
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public byte[] getData() {
    return data;
  }
}