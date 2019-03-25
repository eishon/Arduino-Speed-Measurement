#include <SoftwareSerial.h>
const int RX_PIN = 0;
const int TX_PIN = 1;
SoftwareSerial bt(RX_PIN, TX_PIN);

int sensVal_1 = 0; 
int sensVal_2 = 0;

float d=1.0;

boolean trig_1=false;
boolean trig_2=false;

boolean trig_1_chck=false;

boolean shut_1=false;
boolean shut_2=false;

int time_count=0;
int time_dif=0;
int reset_time=0;
int tm_round=0;
int tm_interval=2000;

int trig_point_1=0;
int trig_point_2=0;

float calculated_speed=0.0;

String data="";

void setup() {
  Serial.begin(9600);
  bt.begin (9600);
}

void loop() {

  if(time_count>=tm_interval) {
    tm_round+=1;
    time_count=0;
    //Serial.println(" time count reset ");
  }

  if(!trig_1){
    if((tm_interval*tm_round)>=10000){
      reset();
    }
  }
  
  sensVal_1 = analogRead(A0);
  sensVal_2 = analogRead(A1);

  sensor_response_1();

  if(trig_1_chck){
    reset_time+=1;
    if(reset_time>=3000){
     // Serial.println(" trigger 1 reset ");
      reset();
    }
  }

  sensor_response_2();

  if(trig_1 && trig_2){

    calculate_time_dif(trig_point_1,trig_point_2);

    if(time_dif==0){
      calculated_speed=0.0;
    }else{
      calculated_speed=(d*1000)/time_dif;
    }

    //bt.print(calculated_speed);
    //Serial.print(calculated_speed);
    /*Serial.print("speed: ");
    Serial.print(calculated_speed);
    Serial.print("\n");*/
    data="";
    data+=d;
    data+=" ";
    data+=time_dif;
    data+=" ";
    data+=calculated_speed;
    //data+="m/s";
    Serial.println(data);
  
    reset();
  }

  time_count+=1;
  delay(1);
}

void reset() {
  trig_1=false;
  trig_2=false;
  trig_1_chck=false;
  shut_1=false;
  shut_2=false;
  reset_time=0;
  time_count=0;
  tm_round=0;
  calculated_speed=0.0;
  trig_point_1=0;
  trig_point_2=0;
  //Serial.println("RESET");
}

void sensor_response_1() {
    if(sensVal_1 <= 700){
      if(!trig_1){
        trig_point_1=(tm_interval*tm_round)+time_count;
        /*Serial.println("trigger 1 - ");
        Serial.print(trig_point_1);
        Serial.println("   ");*/
      }
      trig_1=true;
      trig_1_chck=true;
  }
}

void sensor_response_2() {
  if(trig_1){
    if(sensVal_2 <= 700){
      if(!trig_2){
        trig_point_2=(tm_interval*tm_round)+time_count;
        /*Serial.print("trigger 2 - ");
        Serial.print(trig_point_2);
        Serial.println("   ");*/
      }
      trig_2=true;
    }
  }
  
}

void calculate_time_dif(int trigger_1,int trigger_2){
  
  time_dif=trigger_2-trigger_1;
  
  /*Serial.print("time dif - ");
  Serial.print(time_dif);
  Serial.println("   ");*/
}

