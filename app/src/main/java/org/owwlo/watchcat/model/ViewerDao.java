package org.owwlo.watchcat.model;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ViewerDao {
    @Query("SELECT * FROM viewer")
    List<Viewer> getAll();

    @Query("SELECT * FROM viewer WHERE id IN (:viewerIds)")
    List<Viewer> loadAllByIds(int[] viewerIds);

    @Query("SELECT * FROM viewer WHERE myself = 0")
    List<Viewer> loadClientViewers();

    @Query("SELECT * FROM viewer WHERE id = :id and myself = 0 LIMIT 1")
    Viewer findClientById(String id);

    @Query("SELECT * FROM viewer WHERE myself = 1 LIMIT 1")
    Viewer findMyself();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(Viewer... viewers);

    @Delete
    void delete(Viewer viewer);
}
