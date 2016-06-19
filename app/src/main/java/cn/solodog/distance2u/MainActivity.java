package cn.solodog.distance2u;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.overlay.PoiOverlay;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.RouteSearch.OnRouteSearchListener;
import com.amap.api.services.route.WalkRouteResult;


import java.util.List;

public class MainActivity extends AppCompatActivity implements LocationSource,
        AMapLocationListener, PoiSearch.OnPoiSearchListener,
        SearchView.OnQueryTextListener, AMap.OnMapClickListener, AMap.OnMarkerClickListener, OnRouteSearchListener {
    private MapView mapView;
    private AMap aMap;
    Toolbar toolbar;
    private Context mContext;
    private ProgressDialog progDialog = null;
    private OnLocationChangedListener mListener;
    private AMapLocationClient mlocationClient;
    private AMapLocationClientOption mLocationOption;
    private MyLocationStyle locationStyle;
    private int currentPage = 0;// 当前页面，从0开始计数
    private PoiSearch.Query query;// Poi查询条件类
    private PoiSearch poiSearch;// POI搜索
    private PoiResult poiResult; // poi返回的结果
    private String KeyWord = "";
    private LatLonPoint mStartPoint = new LatLonPoint(0, 0);//起点
    private LatLonPoint mEndPoint = new LatLonPoint(0, 0);//终点
    private RouteSearch mRouteSearch;
    private DriveRouteResult mDriveRouteResult;
    private int seted;
    private SearchView sv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapView = (MapView) findViewById(R.id.map);
        toolbar = (Toolbar) findViewById(R.id.mytoolbar);
        mContext = this.getApplicationContext();
        assert mapView != null;
        mapView.onCreate(savedInstanceState);

        init();
    }

    private void init() {
        if (aMap == null) {
            aMap = mapView.getMap();
            setUpMap();
            registerListener();
        }
    }

    private void setUpMap() {
        aMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        // 设置定位的类型为定位模式 ，可以由定位、跟随或地图根据面向方向旋转几种
        aMap.setMyLocationType(AMap.LOCATION_TYPE_LOCATE);
        locationStyle = new MyLocationStyle();
        locationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.point5));
        locationStyle.strokeColor(Color.BLACK);
        //自定义精度范围的圆形边框宽度
        locationStyle.strokeWidth(1);
        aMap.setMyLocationStyle(locationStyle);
        toolbar.inflateMenu(R.menu.ic_menu);
        toolbar.setTitle("距离");
        seted = 0;
    }

    private void registerListener() {
        aMap.setLocationSource(this);// 设置定位监听
        aMap.setOnMarkerClickListener(this);//marker点击监听
        aMap.setOnMapClickListener(this);// 对amap添加单击地图事件监听器
        mRouteSearch = new RouteSearch(this);
        mRouteSearch.setRouteSearchListener(this);
        sv = (SearchView) findViewById(R.id.action_search);
        sv.setOnQueryTextListener(this);

    }


    //------------------定位start------------------
        public void onLocationChanged(AMapLocation amapLocation) {
        if (mListener != null && amapLocation != null) {
            if (amapLocation != null
                    && amapLocation.getErrorCode() == 0) {
                mListener.onLocationChanged(amapLocation);// 显示系统小蓝点
                deactivate();
            } else {
                String errText = "定位失败," + amapLocation.getErrorCode() + ": " + amapLocation.getErrorInfo();
                Log.e("AmapErr", errText);
            }
        }
    }

    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
        if (mlocationClient == null) {
            mlocationClient = new AMapLocationClient(this);
            mLocationOption = new AMapLocationClientOption();
            //设置定位监听
            mlocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Battery_Saving);
            //设置定位参数
            mlocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mlocationClient.startLocation();
        }
    }

    public void deactivate() {
        mListener = null;
        if (mlocationClient != null) {
            mlocationClient.stopLocation();
            mlocationClient.onDestroy();
        }
        mlocationClient = null;
    }

    private void showProgressDialog() {
        if (progDialog == null)
            progDialog = new ProgressDialog(this);
        progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progDialog.setIndeterminate(false);
        progDialog.setCancelable(false);
        progDialog.setMessage("正在搜索:");
        progDialog.show();
    }
//------------------定位end------------------

    /**
     * 隐藏进度框
     */
    //------------------查询start------------------
    private void dissmissProgressDialog() {
        if (progDialog != null) {
            progDialog.dismiss();
        }
    }

    public boolean onQueryTextSubmit(String query) {
        KeyWord = query;
        if ("".equals(query)) {
            Toast.makeText(MainActivity.this, "请输入搜索关键字", Toast.LENGTH_SHORT).show();
            return true;
        } else {
            doSearchQuery();
        }

        return false;
    }

    protected void doSearchQuery() {
        showProgressDialog();// 显示进度框
        currentPage = 0;
        query = new PoiSearch.Query(KeyWord, "", "");// 第一个参数表示搜索字符串，第二个参数表示poi搜索类型，第三个参数表示poi搜索区域（空字符串代表全国）
        query.setPageSize(30);// 设置每页最多返回多少条poiitem
        query.setPageNum(currentPage);// 设置查第一页
        poiSearch = new PoiSearch(this, query);
        poiSearch.setOnPoiSearchListener(this);
        poiSearch.searchPOIAsyn();
    }


    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public void onPoiSearched(PoiResult result, int rCode) {
        dissmissProgressDialog();// 隐藏对话框
        if (rCode == 1000) {
            if (result != null && result.getQuery() != null) {// 搜索poi的结果
                if (result.getQuery().equals(query)) {// 是否是同一条
                    poiResult = result;
                    // 取得搜索到的poiitems有多少页
                    List<PoiItem> poiItems = poiResult.getPois();// 取得第一页的poiitem数据，页数从数字0开始
                    if (poiItems != null && poiItems.size() > 0) {
                        aMap.clear();// 清理之前的图标
                        PoiOverlay poiOverlay = new PoiOverlay(aMap, poiItems);
//                        poiOverlay.removeFromMap();
//                        poiOverlay.addToMap();
                        poiOverlay.zoomToSpan();
                    } else {
                        Toast.makeText(MainActivity.this, "无结果", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(MainActivity.this, "无结果", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, rCode, Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {

    }

    public boolean onMarkerClick(Marker marker) {
        if(marker.getPosition().latitude==mStartPoint.getLatitude()&&marker.getPosition().longitude==mStartPoint.getLongitude())
        {
            reset();
        }
        return true;
    }
    //------------------查询end------------------
    //------------------路径start----------------
    @Override
    public void onMapClick(LatLng latLng) {
        if (seted == 0) {
            mStartPoint.setLatitude(latLng.latitude);
            mStartPoint.setLongitude(latLng.longitude);
            aMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.fromResource(R.drawable.amap_start)));
            seted = 1;
            Toast.makeText(mContext, "起点已设置", Toast.LENGTH_SHORT).show();
        } else if (seted == 1) {
            mEndPoint.setLatitude(latLng.latitude);
            mEndPoint.setLongitude(latLng.longitude);
            aMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.fromResource(R.drawable.amap_end)));
            seted = 2;
            searchRouteResult();
        } else {
            Toast.makeText(mContext, "请先重置起点", Toast.LENGTH_SHORT).show();
        }
    }


    public void searchRouteResult() {
        if (mStartPoint.getLatitude() == 0) {
            ToastUtil.show(mContext, "请点击地图选择起点");
            return;
        }
        if (mEndPoint.getLatitude() == 0) {
            ToastUtil.show(mContext, "请点击地图选择终点");
            return;
        }
        showProgressDialog();
        final RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(
                mStartPoint, mEndPoint);
        RouteSearch.DriveRouteQuery query = new RouteSearch.DriveRouteQuery(fromAndTo, RouteSearch.DrivingDefault, null,
                null, "");// 第一个参数表示路径规划的起点和终点，第二个参数表示驾车模式，第三个参数表示途经点，第四个参数表示避让区域，第五个参数表示避让道路
        mRouteSearch.calculateDriveRouteAsyn(query);// 异步路径规划驾车模式查询

    }

    @Override
    public void onBusRouteSearched(BusRouteResult busRouteResult, int i) {

    }

    @Override
    public void onDriveRouteSearched(DriveRouteResult result, int errorCode) {
        dissmissProgressDialog();
        if (errorCode == 1000) {
            if (result != null && result.getPaths() != null) {
                if (result.getPaths().size() > 0) {
                    mDriveRouteResult = result;
                    final DrivePath drivePath = mDriveRouteResult.getPaths()
                            .get(0);
                    int dis = (int) drivePath.getDistance();
//                    Toast.makeText(mContext, "两点的距离是" + dis + "米", Toast.LENGTH_SHORT).show();
                    Intent intent=new Intent(MainActivity.this,ShareDis.class);
                    intent.putExtra("dis",dis);
                    startActivity(intent);
                    reset();
                } else if (result != null && result.getPaths() == null) {
                    ToastUtil.show(mContext, "没有搜索到相关数据");
                }

            } else {
                ToastUtil.show(mContext, "没有搜索到相关数据");
            }
        } else {
            ToastUtil.showerror(this.getApplicationContext(), errorCode);
        }
    }

    @Override
    public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int i) {

    }
    public void reset() {
        seted = 0;
        aMap.clear();
        mStartPoint.setLatitude(0);
        mEndPoint.setLatitude(0);
    }


    //------------------路径end------------------
    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        deactivate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (null != mlocationClient) {
            mlocationClient.onDestroy();
        }
    }
}

