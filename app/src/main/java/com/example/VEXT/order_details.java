package com.example.VEXT;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.biometrics.BiometricPrompt;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;


import android.os.CancellationSignal;
import android.telephony.TelephonyManager;
import android.view.View;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.SetOptions;


import java.io.Serializable;


import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


import io.paperdb.Paper;

public class order_details extends AppCompatActivity implements Serializable {
 

    private TextView tv, tv9;
    private TextView tv1;
    private ListView ls;


    private FusedLocationProviderClient fusedLocationProviderClient;

    private Location currentLocation;


    private Button confirm;
    private boolean regular;
    private List<Data> list = new ArrayList<>();

    private ProgressDialog progressDialog;

    private ArrayAdapter<String> ordered_items;

    private String CURRENT_ORDERS = "Current_Orders";
    private String mPhoneNumber;
    static final int PERMISSION_READ_STATE = 123;
    private Double total = 0.0;

    FirebaseUser user;
    float[] dist = new float[10];
    GeoPoint gp;


    List<String> itemlist = new ArrayList<>();

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection(CURRENT_ORDERS);
    private CollectionReference users = db.collection("users");
    private DocumentReference document;

    @Override
    protected void onStart() {
        super.onStart();
        DocumentReference docref =db.collection("users").document(FirebaseAuth.getInstance().getCurrentUser().getUid());
        docref.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                final boolean regular =documentSnapshot.getBoolean("regular");

                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(order_details.this);
                try {
                    final Task location = fusedLocationProviderClient.getLastLocation();
                    location.addOnCompleteListener(new OnCompleteListener() {
                        @Override
                        public void onComplete(@NonNull Task task) {
                            if (task.isSuccessful()) {
                                currentLocation = (Location) task.getResult();

                                if(currentLocation==null)
                                {
                                    Toast.makeText(order_details.this, "Location Not Found", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                            }

                            if(currentLocation!=null) {

                                Location.distanceBetween(gp.getLatitude(), gp.getLongitude(), currentLocation.getLatitude(), currentLocation.getLongitude(), dist);
                            }
                            total = Double.parseDouble(String.format("%.3f",total));
                            tv1.append(String.valueOf(total));


                            /* adapter = new OrderAdapter(order_details.this, R.layout.list_order_items, itemlist);*/
                            ArrayAdapter<String> adapter=new ArrayAdapter<String>(order_details.this,android.R.layout.simple_list_item_1,itemlist);
                            ls.setAdapter(adapter);

                        }
                    });
                }
                catch(Exception e) {
                    e.printStackTrace();

                }
            }
        });

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);


        final Double lat = getIntent().getExtras().getDouble("eatery_location_latitude");
        final Double lon = getIntent().getExtras().getDouble("eatery_location_longitude");
        gp = new GeoPoint(lat, lon);
        user = FirebaseAuth.getInstance().getCurrentUser();

        ordered_items = new ArrayAdapter<String>(order_details.this, android.R.layout.simple_list_item_1);

        int permissionCheck = ContextCompat.checkSelfPermission(order_details.this, Manifest.permission.READ_PHONE_STATE);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            TelephonyManager tMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
            mPhoneNumber = tMgr.getLine1Number();
        } else {
            ActivityCompat.requestPermissions(order_details.this, new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSION_READ_STATE);
        }


        tv = (TextView)findViewById(R.id.textView6);
        tv1 = (TextView)findViewById(R.id.textView7);
        ls = (ListView)findViewById(R.id.listView22);
        confirm = (Button)findViewById(R.id.button5);

        Paper.init(order_details.this);

        List<String> allKeys = Paper.book().getAllKeys();

        Data d = new Data();
        for(String key: allKeys)
        {
            d = Paper.book().read(key);
            Paper.book().delete("contacts");

            if(d.items!=0)
            {
                list.add(d);
                ordered_items.add(String.valueOf(d.items)+ " " + d.name + " @ Rs." + String.valueOf(d.price) );
                total += d.price*d.items;

                itemlist.add("Item name: "+ d.name+"\n" +"Price: "+d.price+"\n"+"Quantity: "+d.items+"\n"+"Sub-Amount: "+ d.price*d.items+"\n");

            }

        }

        total = Double.parseDouble(String.format("%.3f", total));




        Intent intent = order_details.this.getIntent();
        final String eatery_id = intent.getStringExtra("eatery_id");
        final String eatery_name = intent.getStringExtra("eatery_name");

        Paper.init(order_details.this);
        Paper.book().destroy();


        confirm.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void onClick(View v) {

                        order_details.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                Map<String,Double> map = new HashMap();

                                for(Data dat:list)
                                {
                                    map.put(dat.name+"$"+dat.price, (double) dat.items);
                                }








                                String uid =  user.getUid();
                                String uname = user.getDisplayName();
                                String uemail = user.getEmail();




                                CurrentOrder currentOrder = new CurrentOrder( map , uid, total, eatery_id, eatery_name, gp, uname, uemail, mPhoneNumber);

                                Map<String, String> user_info = new HashMap<>();
                                user_info.put("user_name",FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
                                user_info.put("user_email", FirebaseAuth.getInstance().getCurrentUser().getEmail());
                                FirebaseFirestore.getInstance().collection("Users").document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                        .set(user_info, SetOptions.merge());

                                 collectionReference.add(currentOrder).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
//                                        Toast.makeText(order_details.this, "Success", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(order_details.this, OrderConfirm.class);
                                        intent.putExtra("order_id", documentReference.getId());
                                        intent.putExtra("user", user);
                                        startActivity(intent);
                                        order_details.this.finish();

                                    }
                                });
                            }
                        });


                    }
                });


            };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }



    }
