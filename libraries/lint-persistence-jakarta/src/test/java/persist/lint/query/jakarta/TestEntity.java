package persist.lint.query.jakarta;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "test_entity")
@NamedQuery(name = "TestEntity.findByName", query = "SELECT e FROM TestEntity e WHERE e.name = :name")
public class TestEntity {

    @Id
    private Long id;
    private String name;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

}
