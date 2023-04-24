package com.lundong.metabitorgsync.service;

import com.lundong.metabitorgsync.entity.ExcelDept;

import java.util.List;

public interface SystemService {

    String initDepartment();

    String initUser();

    List<ExcelDept> parseExcel();

}
