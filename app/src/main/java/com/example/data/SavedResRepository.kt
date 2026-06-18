package com.example.data

import kotlinx.coroutines.flow.Flow

class SavedResRepository(private val savedResDao: SavedResDao) {

  val allSaved: Flow<List<SavedRes>> = savedResDao.getAllSaved()

  suspend fun getById(resourceId: String): SavedRes? {
    return savedResDao.getById(resourceId)
  }

  suspend fun save(savedRes: SavedRes) {
    savedResDao.insert(savedRes)
  }

  suspend fun update(savedRes: SavedRes) {
    savedResDao.update(savedRes)
  }

  suspend fun deleteById(resourceId: String) {
    savedResDao.deleteById(resourceId)
  }
}
